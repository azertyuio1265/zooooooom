const express = require('express');
const router = express.Router();
const { supabase } = require('../config/database');
const { authenticate, authorize } = require('../middleware/auth');
const logger = require('../utils/logger');

// Get all published blog posts
router.get('/blogs', async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('blogs')
            .select('*')
            .order('created_at', { ascending: false });

        if (error) {
            // If table doesn't exist yet, return empty array gracefully
            if (error.code === '42P01') {
                return res.json({ success: true, blogs: [] });
            }
            throw error;
        }

        res.json({ success: true, blogs: data || [] });
    } catch (e) {
        logger.error('Error fetching blogs:', e);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Get single blog post by slug or id
router.get('/blogs/:identifier', async (req, res) => {
    try {
        const identifier = decodeURIComponent(req.params.identifier);
        
        // Try searching by slug first
        let { data, error } = await supabase
            .from('blogs')
            .select('*')
            .eq('slug', identifier)
            .maybeSingle();

        // If not found by slug and identifier is numeric, try searching by id
        if (!data && !isNaN(identifier) && Number.isInteger(Number(identifier))) {
            const idRes = await supabase
                .from('blogs')
                .select('*')
                .eq('id', parseInt(identifier))
                .maybeSingle();
            data = idRes.data;
            error = idRes.error;
        }

        if (error || !data) {
            return res.status(404).json({ success: false, error: 'المقال غير موجود' });
        }

        res.json({ success: true, blog: data });
    } catch (e) {
        logger.error('Error fetching single blog:', e);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Admin: Create blog post
router.post('/admin/blogs', authenticate, authorize(['admin']), async (req, res) => {
    try {
        const { title, slug, excerpt, content, cover_image, seo_keywords, meta_description, author } = req.body;

        if (!title || !content) {
            return res.status(400).json({ success: false, error: 'العنوان والمحتوى مطلوبان' });
        }

        const finalSlug = slug ? slug.trim().toLowerCase().replace(/\s+/g, '-') : title.trim().toLowerCase().replace(/[^\w\s-]/g, '').replace(/\s+/g, '-');

        const newBlog = {
            title,
            slug: finalSlug,
            excerpt: excerpt || '',
            content,
            cover_image: cover_image || '',
            seo_keywords: seo_keywords || '',
            meta_description: meta_description || excerpt || '',
            author: author || 'إدارة ZoomDz',
            created_at: new Date().toISOString()
        };

        const { data, error } = await supabase
            .from('blogs')
            .insert([newBlog])
            .select()
            .single();

        if (error) {
            // If table doesn't exist, create instruction or error
            if (error.code === '42P01') {
                return res.status(400).json({ success: false, error: 'جدول المقالات (blogs) غير موجود في قاعدة البيانات' });
            }
            throw error;
        }

        res.json({ success: true, blog: data });
    } catch (e) {
        logger.error('Error creating blog:', e);
        res.status(500).json({ success: false, error: e.message || 'Internal server error' });
    }
});

// Admin: Delete blog post
router.delete('/admin/blogs/:id', authenticate, authorize(['admin']), async (req, res) => {
    try {
        const blogId = req.params.id;
        const { error } = await supabase
            .from('blogs')
            .delete()
            .eq('id', blogId);

        if (error) throw error;

        res.json({ success: true, message: 'تم حذف المقال بنجاح' });
    } catch (e) {
        logger.error('Error deleting blog:', e);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

module.exports = router;
