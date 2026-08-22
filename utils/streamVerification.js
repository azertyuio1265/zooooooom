const logger = require('./logger');
// ============================================================
// نظام التحقق المستقل من وقت البث - Stream Verification System
// ============================================================

const { supabase } = require('../config/database');
const axios = require('axios');

// ============================================================
// دوال التحقق من Jitsi
// ============================================================

/**
 * التحقق من غرفة Jitsi وجمع معلوماتها
 * ملاحظة: Jitsi لا يوفر API عام للتحقق من الاجتماعات
 * لذلك نستخدم نظامنا الداخلي للتحقق
 */
async function verifyJitsiRoom(roomName) {
    try {
        // التحقق من وجود الغرفة في جدول الدروس
        const { data: offer, error } = await supabase
            .from('offers')
            .select('id, room_name, stream_url, status, stream_started_at')
            .eq('room_name', roomName)
            .single();

        if (error || !offer) {
            return { valid: false, error: 'الغرفة غير موجودة' };
        }

        // التحقق من حالة البث
        if (offer.status !== 'live' && offer.status !== 'paused') {
            return { valid: false, error: 'البث ليس نشطاً' };
        }

        return {
            valid: true,
            offer: offer,
            roomActive: true
        };
    } catch (error) {
        logger.error('خطأ في التحقق من Jitsi:', error.message);
        return { valid: false, error: error.message };
    }
}

/**
 * تسجيل بداية البث المستقلة (timestamp من الخادم)
 */
async function recordStreamStart(offerId, teacherId) {
    const serverTimestamp = new Date().toISOString();

    const { data, error } = await supabase
        .from('stream_verification')
        .insert({
            offer_id: offerId,
            teacher_id: teacherId,
            server_start_time: serverTimestamp,
            status: 'started',
            created_at: serverTimestamp
        })
        .select()
        .single();

    if (error) {
        logger.error('خطأ في تسجيل بداية البث:', error.message);
        // إذا فشل الإدراج، حاول التحديث
        await supabase
            .from('stream_verification')
            .update({
                server_start_time: serverTimestamp,
                status: 'started'
            })
            .eq('offer_id', offerId)
            .eq('teacher_id', teacherId);
    }

    return data;
}

/**
 * تسجيل إيقاف البث مؤقتاً
 */
async function recordStreamPause(offerId) {
    const serverTimestamp = new Date().toISOString();

    const { data, error } = await supabase
        .from('stream_verification')
        .update({
            last_pause_time: serverTimestamp,
            total_paused_seconds: supabase.rpc('add_seconds', {
                current: supabase.rpc('get_total_paused', { offer_id: offerId }),
                add: serverTimestamp
            })
        })
        .eq('offer_id', offerId)
        .select()
        .single();

    return data;
}

/**
 * حساب الوقت الفعلي للبث من الخادم
 */
async function calculateActualStreamDuration(offerId) {
    const { data: verification, error } = await supabase
        .from('stream_verification')
        .select('*')
        .eq('offer_id', offerId)
        .single();

    if (error || !verification) {
        return null;
    }

    const startTime = new Date(verification.server_start_time);
    const endTime = verification.server_end_time 
        ? new Date(verification.server_end_time) 
        : new Date();
    
    const totalSeconds = Math.floor((endTime - startTime) / 1000);
    
    // حساب وقت الإيقاف الكلي (يمكن تحسينه لاحقاً)
    const pausedSeconds = verification.total_paused_seconds || 0;
    
    // الوقت الفعلي للبث = الوقت الكلي - وقت الإيقاف
    const actualLiveSeconds = Math.max(0, totalSeconds - pausedSeconds);

    return {
        total_seconds: totalSeconds,
        paused_seconds: pausedSeconds,
        actual_live_seconds: actualLiveSeconds,
        started_at: verification.server_start_time,
        ended_at: verification.server_end_time
    };
}

/**
 * إنهاء البث وتسجيل الوقت النهائي
 */
async function recordStreamEnd(offerId, teacherId) {
    const serverTimestamp = new Date().toISOString();

    // البحث عن سجل التحقق
    const { data: existing, error: findError } = await supabase
        .from('stream_verification')
        .select('*')
        .eq('offer_id', offerId)
        .single();

    let updateData = {
        server_end_time: serverTimestamp,
        status: 'completed'
    };

    // حساب الوقت الفعلي للبث
    if (existing && existing.server_start_time) {
        const startTime = new Date(existing.server_start_time);
        const endTime = new Date(serverTimestamp);
        const totalSeconds = Math.floor((endTime - startTime) / 1000);
        
        updateData.total_duration_seconds = totalSeconds;
        
        // حساب وقت الإيقاف
        const pausedSeconds = existing.total_paused_seconds || 0;
        updateData.actual_live_seconds = Math.max(0, totalSeconds - pausedSeconds);
    }

    if (existing) {
        // تحديث السجل الموجود
        const { data, error } = await supabase
            .from('stream_verification')
            .update(updateData)
            .eq('offer_id', offerId)
            .select()
            .single();

        return data;
    } else {
        // إنشاء سجل جديد إذا لم يكن موجوداً
        const { data, error } = await supabase
            .from('stream_verification')
            .insert({
                offer_id: offerId,
                teacher_id: teacherId,
                server_start_time: serverTimestamp,
                server_end_time: serverTimestamp,
                total_duration_seconds: 0,
                actual_live_seconds: 0,
                status: 'completed',
                created_at: serverTimestamp
            })
            .select()
            .single();

        return data;
    }
}

/**
 * التحقق من اكتمال البث للمحفظة
 */
async function verifyStreamCompletion(offerId) {
    const { data: offer, error: offerError } = await supabase
        .from('offers')
        .select('id, duration, teacher_id, subject_name, price')
        .eq('id', offerId)
        .single();

    if (offerError || !offer) {
        return { complete: false, error: 'الدرس غير موجود' };
    }

    const verification = await calculateActualStreamDuration(offerId);
    
    if (!verification) {
        return { complete: false, error: 'لا توجد بيانات تحقق' };
    }

    const expectedDuration = offer.duration * 60; // بالدقائق إلى ثواني
    const actualDuration = verification.actual_live_seconds;
    const completionPercentage = (actualDuration / expectedDuration) * 100;

    return {
        complete: completionPercentage >= 80, // 80% من الوقت المطلوب
        completion_percentage: completionPercentage,
        expected_seconds: expectedDuration,
        actual_seconds: actualDuration,
        shortfall_seconds: Math.max(0, expectedDuration - actualDuration)
    };
}

/**
 * معالجة المدفوعات حسب وقت البث الفعلي
 */
async function processStreamPayments(offerId, earlyEnd = false) {
    
    // إذا كان إنهاء مبكر، استرداد كامل للطلاب
    if (earlyEnd) {
        return await processEarlyEndRefund(offerId);
    }
    
    // Otherwise, process normal completion with partial payments
    const completion = await verifyStreamCompletion(offerId);
    
    const { data: offer, error: offerError } = await supabase
        .from('offers')
        .select('id, teacher_id, price, subject_name, is_free')
        .eq('id', offerId)
        .single();

    if (offerError || !offer) {
        logger.error('خطأ في جلب الدرس:', offerError);
        return;
    }

    // إذا كان مجانياً، لا حاجة للمعالجة
    const isOfferFree = offer ? (offer.is_free === true || offer.is_free === 'true' || offer.is_free === 1 || offer.price === 0 || parseFloat(offer.price) === 0) : false;
    if (isOfferFree) {
        console.log('الدرس مجاني، لا حاجة لمعالجة المدفوعات');
        return;
    }

    // جلب جميع الجلسات المعلقة لهذا الدرس
    const { data: sessions, error: sessionsError } = await supabase
        .from('sessions')
        .select('id, student_id, payment_amount, payment_status')
        .eq('offer_id', offerId)
        .eq('payment_status', 'pending_stream');

    if (sessionsError) {
        logger.error('خطأ في جلب الجلسات:', sessionsError);
        return;
    }

    console.log(`📊 جاري معالجة ${sessions?.length || 0} جلسة للبث ${offerId}`);

    for (const session of (sessions || [])) {
        // إذا كان البث مكتمل بنسبة 90% فما فوق، يعتبر مكتملاً ولا يوجد استرداد
        const isCompleted = completion.completion_percentage >= 90;

        if (isCompleted) {
            // البث مكتمل - لا استرداد للطالب، الأستاذ يحصل على سعر الحصة
            const teacherAmount = isOfferFree ? 0 : (offer.price || 0);

            // تحديث حالة الجلسة
            await supabase
                .from('sessions')
                .update({
                    payment_status: 'paid',
                    teacher_earned: teacherAmount,
                    completed_at: new Date().toISOString()
                })
                .eq('id', session.id);

            // إضافة للأستاذ
            if (teacherAmount > 0) {
                const { data: teacher } = await supabase
                    .from('teachers')
                    .select('pending_withdraw, total_earned')
                    .eq('id', offer.teacher_id)
                    .single();

                await supabase
                    .from('teachers')
                    .update({
                        pending_withdraw: Math.max(0, (teacher?.pending_withdraw || 0) - teacherAmount),
                        total_earned: (teacher?.total_earned || 0) + teacherAmount
                    })
                    .eq('id', offer.teacher_id);
                
                console.log(`✅ تم تحويل ${teacherAmount} دج للأستاذ (بث مكتمل)`);
            }
        } else {
            // البث غير مكتمل (أقل من 90%) - استرداد كامل للطالب في الحصص المدفوعة، لا شيء للأستاذ
            const teacherAmount = 0;
            const refundAmount = Math.max(0, session.payment_amount - 100); // استرداد مبلغ الحصه فقط بدون رسوم 100

            // تحديث حالة الجلسة
            await supabase
                .from('sessions')
                .update({
                    payment_status: 'refunded',
                    teacher_earned: teacherAmount,
                    completed_at: new Date().toISOString(),
                    partial_payment_note: `استرداد كامل - البث لم يكتمل نسبة 90% (النسبة: ${Math.round(completion.completion_percentage)}%)`
                })
                .eq('id', session.id);

            // إزالة المبلغ المعلق من الأستاذ إذا كان قد أضيف له
            const originalTeacherEarned = isOfferFree ? 0 : (offer.price || 0);
            if (originalTeacherEarned > 0) {
                const { data: teacher } = await supabase
                    .from('teachers')
                    .select('pending_withdraw')
                    .eq('id', offer.teacher_id)
                    .single();

                await supabase
                    .from('teachers')
                    .update({
                        pending_withdraw: Math.max(0, (teacher?.pending_withdraw || 0) - originalTeacherEarned)
                    })
                    .eq('id', offer.teacher_id);
            }

            if (refundAmount > 0) {
                // استرداد المبلغ للطالب
                const { data: student } = await supabase
                    .from('students')
                    .select('wallet_balance')
                    .eq('id', session.student_id)
                    .single();

                await supabase
                    .from('students')
                    .update({
                        wallet_balance: (student?.wallet_balance || 0) + refundAmount
                    })
                    .eq('id', session.student_id);

                // تسجيل المعاملة
                await supabase
                    .from('wallet_transactions')
                    .insert({
                        student_id: session.student_id,
                        amount: refundAmount,
                        type: 'refund',
                        status: 'completed',
                        description: `استرداد كامل ${refundAmount} دج - البث لم يكتمل نسبة 90% (${Math.round(completion.completion_percentage)}% فقط)`,
                        created_at: new Date().toISOString()
                    });

                console.log(`💰 تم استرداد كامل ${refundAmount} دج للطالب`);

                // إشعار الطالب بالاسترداد
                await supabase
                    .from('notifications')
                    .insert({
                        user_id: session.student_id,
                        user_type: 'student',
                        title: '💰 استرداد كامل',
                        message: `تم استرداد كامل المبلغ (${refundAmount} دج) لحصة "${offer.subject_name}" بسبب عدم اكتمال البث (${Math.round(completion.completion_percentage)}% فقط).`,
                        is_read: false,
                        created_at: new Date().toISOString()
                    });
            }
        }

        // إشعار الأستاذ
        await supabase
            .from('notifications')
            .insert({
                user_id: offer.teacher_id,
                user_type: 'teacher',
                title: '📊 تقرير البث',
                message: `تم إنهاء البث "${offer.subject_name}". نسبة الاكتمال: ${Math.round(completion.completion_percentage)}%`,
                is_read: false,
                created_at: new Date().toISOString()
            });
    }
}

/**
 * معالجة الاسترداد الكامل عند الإنهاء المبكر
 * - لا يحصل الأستاذ على أي مال
 * - يتم استرداد جميع الأموال للطلاب
 */
async function processEarlyEndRefund(offerId) {
    const { data: offer, error: offerError } = await supabase
        .from('offers')
        .select('id, teacher_id, subject_name, is_free, price')
        .eq('id', offerId)
        .single();

    if (offerError || !offer) {
        logger.error('خطأ في جلب الدرس للاسترداد:', offerError);
        return;
    }

    // إذا كان مجانياً، لا حاجة للمعالجة
    const isOfferFree = offer ? (offer.is_free === true || offer.is_free === 'true' || offer.is_free === 1 || offer.price === 0 || parseFloat(offer.price) === 0) : false;
    if (isOfferFree) {
        console.log('الدرس مجاني، لا حاجة لمعالجة الاسترداد');
        return;
    }

    // جلب جميع الجلسات المعلقة
    const { data: sessions, error: sessionsError } = await supabase
        .from('sessions')
        .select('id, student_id, payment_amount, payment_status')
        .eq('offer_id', offerId)
        .eq('payment_status', 'pending_stream');

    if (sessionsError) {
        logger.error('خطأ في جلب الجلسات:', sessionsError);
        return;
    }

    console.log(`⚠️ معالجة إنهاء مبكر للبث ${offerId} - استرداد كامل للطلاب`);

    for (const session of (sessions || [])) {
        // التحقق الإضافي من الحالة قبل المعالجة (تجنب التكرار)
        const { data: currentSession } = await supabase
            .from('sessions')
            .select('payment_status')
            .eq('id', session.id)
            .single();
            
        if (!currentSession || currentSession.payment_status !== 'pending_stream') {
            console.log(`ℹ️ الجلسة ${session.id} تم استردادها بالفعل أو تغيرت حالتها`);
            continue;
        }

        // استرداد كامل للمبلغ للطالب (باستثناء رسوم السيرفر 100)
        const { data: student } = await supabase
            .from('students')
            .select('wallet_balance')
            .eq('id', session.student_id)
            .single();

        await supabase
            .from('students')
            .update({
                wallet_balance: (student?.wallet_balance || 0) + Math.max(0, session.payment_amount - 100)
            })
            .eq('id', session.student_id);

        // تحديث حالة الجلسة
        await supabase
            .from('sessions')
            .update({
                payment_status: 'refunded',
                teacher_earned: 0,
                completed_at: new Date().toISOString(),
                partial_payment_note: 'استرداد مبلغ الحصة - أنهى الأستاذ البث مبكراً'
            })
            .eq('id', session.id);

        // تسجيل المعاملة
        await supabase
            .from('wallet_transactions')
            .insert({
                student_id: session.student_id,
                amount: Math.max(0, session.payment_amount - 100),
                type: 'refund',
                status: 'completed',
                description: `استرداد مبلغ الحصة ${Math.max(0, session.payment_amount - 100)} دج - أنهى الأستاذ البث مبكراً`,
                created_at: new Date().toISOString()
            });

        // إشعار الطالب
        await supabase
            .from('notifications')
            .insert({
                user_id: session.student_id,
                user_type: 'student',
                title: '💰 استرداد مبلغ الحصة',
                message: `تم استرداد ${Math.max(0, session.payment_amount - 100)} دج لحصة "${offer.subject_name}" لأن الأستاذ أنهى البث مبكراً`,
                is_read: false,
                created_at: new Date().toISOString()
            });

        console.log(`💰 تم استرداد ${session.payment_amount} دج للطالب ${session.student_id}`);
    }

    // تحديث رصيد الأستاذ المعلق (إلغاء المعلق)
    const { data: teacher } = await supabase
        .from('teachers')
        .select('pending_withdraw')
        .eq('id', offer.teacher_id)
        .single();

    if (teacher?.pending_withdraw > 0) {
        await supabase
            .from('teachers')
            .update({
                pending_withdraw: 0
            })
            .eq('id', offer.teacher_id);
    }

    // إشعار الأستاذ
    await supabase
        .from('notifications')
        .insert({
            user_id: offer.teacher_id,
            user_type: 'teacher',
            title: '⚠️ تم إنهاء البث مبكراً',
            message: `تم إنهاء البث "${offer.subject_name}" مبكراً. لم تحصل على أي مال وتم استرداد جميع المبالغ للطلاب.`,
            is_read: false,
            created_at: new Date().toISOString()
        });

    console.log(`⚠️ تم إنهاء البث مبكراً - لم يحصل الأستاذ على أي مال`);
}

/**
 * جلب بيانات التحقق للبث
 */
async function getStreamVerification(offerId) {
    const { data: verification, error } = await supabase
        .from('stream_verification')
        .select('*')
        .eq('offer_id', offerId)
        .single();

    if (error || !verification) {
        return null;
    }

    const { data: offer } = await supabase
        .from('offers')
        .select('duration')
        .eq('id', offerId)
        .single();

    const completion = await verifyStreamCompletion(offerId);

    return {
        ...verification,
        expected_duration: offer ? offer.duration * 60 : 0,
        completion_percentage: completion.completion_percentage,
        is_complete: completion.complete
    };
}

/**
 * انتهاء الدرس قبل بدئه (فات أوانه أو انقضت مدته بدون بث)
 * يُرجع أموال الطلاب الذين حجزوا ويحذف الدرس نهائياً من قاعدة البيانات
 */
async function expireOverdueOffer(offerId) {
    const { data: offer, error: offerError } = await supabase
        .from('offers')
        .select('id, teacher_id, subject_name, price, is_free, status, offer_date, duration')
        .eq('id', offerId)
        .single();

    if (offerError || !offer) {
        logger.error('expireOverdueOffer: الدرس غير موجود', offerId);
        return;
    }

    if (['completed', 'expired'].includes(offer.status)) return;

    console.log(`⏰ انتهاء الدرس ${offerId} (${offer.subject_name}) - جاري رد الأموال وحذف الدرس`);

    const isOfferFree = offer ? (offer.is_free === true || offer.is_free === 'true' || offer.is_free === 1 || offer.price === 0 || parseFloat(offer.price) === 0) : false;

    if (!isOfferFree) {
        // رد أموال الطلاب الذين دفعوا (paid أو pending_stream)
        const { data: sessions } = await supabase
            .from('sessions')
            .select('id, student_id, payment_amount, payment_status')
            .eq('offer_id', offerId)
            .in('payment_status', ['paid', 'pending_stream']);

        for (const session of (sessions || [])) {
            const { data: student } = await supabase
                .from('students')
                .select('wallet_balance')
                .eq('id', session.student_id)
                .single();

            await supabase
                .from('students')
                .update({ wallet_balance: (student?.wallet_balance || 0) + session.payment_amount })
                .eq('id', session.student_id);

            await supabase
                .from('sessions')
                .update({
                    payment_status: 'refunded',
                    teacher_earned: 0,
                    partial_payment_note: 'استرداد كامل - الدرس انتهى قبل البدء أو فات أوانه'
                })
                .eq('id', session.id);

            await supabase.from('wallet_transactions').insert({
                student_id: session.student_id,
                amount: session.payment_amount,
                type: 'refund',
                status: 'completed',
                description: `استرداد ${session.payment_amount} دج - الدرس "${offer.subject_name}" لم يُقام`,
                created_at: new Date().toISOString()
            });

            await supabase.from('notifications').insert({
                user_id: session.student_id,
                user_type: 'student',
                title: '💰 استرداد تلقائي',
                message: `تم استرداد ${session.payment_amount} دج - لم تُقام حصة "${offer.subject_name}" في الموعد المحدد`,
                is_read: false,
                created_at: new Date().toISOString()
            });

            console.log(`💰 استرداد ${session.payment_amount} دج للطالب ${session.student_id}`);
        }
    }

    // إشعار الأستاذ
    await supabase.from('notifications').insert({
        user_id: offer.teacher_id,
        user_type: 'teacher',
        title: '⏰ تم إلغاء وحذف الدرس تلقائياً',
        message: `الدرس "${offer.subject_name}" تم إلغاؤه وحذفه تلقائياً لأنه لم يُبدأ في الوقت المحدد وانتهت مدته.`,
        is_read: false,
        created_at: new Date().toISOString()
    });

    // ✅ حذف البيانات المرتبطة بالدرس من الجداول التابعة
    const tables = [
        'active_stream', 
        'waiting_room', 
        'student_room_passwords', 
        'stream_verification', 
        'stream_chat_messages', 
        'stream_mutes',
        'sessions'
    ];
    for (const table of tables) {
        try {
            await supabase.from(table).delete().eq('offer_id', offerId);
        } catch (e) {
            logger.error(`expireOverdueOffer: خطأ في حذف بيانات ${table}:`, e.message);
        }
    }

    // ✅ حذف الدرس نفسه من جدول offers
    try {
        await supabase
            .from('offers')
            .delete()
            .eq('id', offerId);
        console.log(`✅ تم حذف الدرس رقم ${offerId} بنجاح من قاعدة البيانات`);
    } catch (e) {
        logger.error('expireOverdueOffer: خطأ في حذف الدرس من جدول offers:', e.message);
    }
}

/**
 * إغلاق البث إجبارياً بعد انتهاء فترة السماح (10 دقائق)
 * يُستدعى من cron أو عند انتهاء grace period
 */
async function forceEndStream(offerId, reason = 'grace_timeout') {
    const { data: offer, error: offerError } = await supabase
        .from('offers')
        .select('id, teacher_id, subject_name, status, price, is_free')
        .eq('id', offerId)
        .single();

    if (offerError || !offer) return;
    if (!['live', 'paused'].includes(offer.status)) return;

    console.log(`🔴 إغلاق إجباري للبث ${offerId} - السبب: ${reason}`);

    // تسجيل نهاية البث
    await recordStreamEndWithReason(offerId, offer.teacher_id, reason);

    const completion = await verifyStreamCompletion(offerId);
    await processStreamPayments(offerId, false);

    await supabase.from('offers').update({
        status: 'completed',
        completed_at: new Date().toISOString(),
        force_ended_at: new Date().toISOString()
    }).eq('id', offerId);

    await supabase.from('active_stream').delete().eq('offer_id', offerId);
    await supabase.from('waiting_room').delete().eq('offer_id', offerId);

    // إشعار الأستاذ
    const reasonMessages = {
        grace_timeout: 'انتهت فترة السماح (10 دقائق) بعد انتهاء وقت الحصة',
        heartbeat_lost: 'غادرت صفحة البث أثناء الحصة',
        expired_offer: 'انتهت مدة الدرس'
    };

    await supabase.from('notifications').insert({
        user_id: offer.teacher_id,
        user_type: 'teacher',
        title: '🔴 تم إغلاق البث تلقائياً',
        message: `تم إغلاق بث "${offer.subject_name}" تلقائياً - ${reasonMessages[reason] || reason}. يرجى إنشاء درس جديد لحصة جديدة.`,
        is_read: false,
        created_at: new Date().toISOString()
    });

    console.log(`✅ تم الإغلاق الإجباري للبث ${offerId}`);
}

/**
 * نسخة من recordStreamEnd مع سبب الإنهاء
 */
async function recordStreamEndWithReason(offerId, teacherId, reason) {
    const serverTimestamp = new Date().toISOString();

    const { data: existing } = await supabase
        .from('stream_verification')
        .select('*')
        .eq('offer_id', offerId)
        .single();

    let updateData = {
        server_end_time: serverTimestamp,
        status: 'completed',
        end_reason: reason
    };

    if (existing?.server_start_time) {
        const totalSeconds = Math.floor((new Date(serverTimestamp) - new Date(existing.server_start_time)) / 1000);
        const pausedSeconds = existing.total_paused_seconds || 0;
        updateData.total_duration_seconds = totalSeconds;
        updateData.actual_live_seconds = Math.max(0, totalSeconds - pausedSeconds);
    }

    if (existing) {
        await supabase.from('stream_verification').update(updateData).eq('offer_id', offerId);
    } else {
        await supabase.from('stream_verification').insert({
            offer_id: offerId,
            teacher_id: teacherId,
            server_start_time: serverTimestamp,
            server_end_time: serverTimestamp,
            total_duration_seconds: 0,
            actual_live_seconds: 0,
            status: 'completed',
            end_reason: reason,
            created_at: serverTimestamp
        });
    }
}

/**
 * اكتشاف الدروس التي فات أوانها أو انتهت مدتها بدون بث
 * يُستدعى من cron كل دقيقة
 */
async function checkAndExpireOverdueOffers() {
    const now = new Date();

    // 1) دروس upcoming فات موعد بدئها + مدتها (أي لم تُبدأ أبداً)
    const { data: overdueOffers } = await supabase
        .from('offers')
        .select('id, offer_date, duration, subject_name')
        .eq('status', 'upcoming');

    for (const offer of (overdueOffers || [])) {
        const offerStart = new Date(offer.offer_date);
        const offerEnd = new Date(offerStart.getTime() + offer.duration * 60 * 1000);
        // إذا انقضى وقت الدرس الكامل ولم يُبدأ
        if (now > offerEnd) {
            await expireOverdueOffer(offer.id);
        }
    }

    // 2) دروس live/paused تجاوزت grace period (10 دقائق بعد انتهاء وقتها)
    const GRACE_MS = 10 * 60 * 1000;
    const { data: liveOffers } = await supabase
        .from('offers')
        .select('id, offer_date, duration, subject_name')
        .in('status', ['live', 'paused']);

    for (const offer of (liveOffers || [])) {
        const offerStart = new Date(offer.offer_date);
        const offerEnd = new Date(offerStart.getTime() + offer.duration * 60 * 1000);
        const graceEnd = new Date(offerEnd.getTime() + GRACE_MS);

        if (now >= graceEnd) {
            console.log(`⏰ انتهت فترة السماح للبث ${offer.id} (${offer.subject_name}) - يتم الإنهاء الإجباري`);
            await forceEndStream(offer.id, 'grace_timeout');
        }
    }
}

module.exports = {
    verifyJitsiRoom,
    recordStreamStart,
    recordStreamPause,
    recordStreamEnd,
    recordStreamEndWithReason,
    calculateActualStreamDuration,
    verifyStreamCompletion,
    processStreamPayments,
    getStreamVerification,
    expireOverdueOffer,
    forceEndStream,
    checkAndExpireOverdueOffers
};
