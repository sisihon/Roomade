const express = require('express');
const router = express.Router();
const db = require('../config/db');

// HTML 엔티티 변환
function decodeHtml(text) {
    return text
        .replace(/&amp;/g, '&')
        .replace(/&quot;/g, '"')
        .replace(/&#39;|&apos;/g, "'")
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&#(\d+);/g, (match, code) =>
            String.fromCharCode(Number(code))
        )
        .replace(/&#x([0-9a-f]+);/gi, (match, code) =>
            String.fromCharCode(parseInt(code, 16))
        )
        .trim();
}

// HTML에서 상품명 추출
function extractProductName(html) {
    const metaTags = html.match(/<meta\b[^>]*>/gi) || [];

    let ogTitle = null;
    let twitterTitle = null;

    for (const tag of metaTags) {
        const propertyMatch = tag.match(
            /\b(?:property|name)\s*=\s*["']([^"']+)["']/i
        );

        const contentMatch = tag.match(
            /\bcontent\s*=\s*["']([^"']*)["']/i
        );

        if (!propertyMatch || !contentMatch) {
            continue;
        }

        const property = propertyMatch[1].toLowerCase();
        const content = decodeHtml(contentMatch[1]);

        if (property === 'og:title') {
            ogTitle = content;
        }

        if (property === 'twitter:title') {
            twitterTitle = content;
        }
    }

    const titleMatch = html.match(
        /<title\b[^>]*>([\s\S]*?)<\/title>/i
    );

    const pageTitle = titleMatch
        ? decodeHtml(titleMatch[1].replace(/<[^>]*>/g, ''))
        : null;

    const candidates = [
        ogTitle,
        twitterTitle,
        pageTitle
    ];

    for (const name of candidates) {
        if (
            name &&
            name.trim() &&
            !['GS Retail', 'GS리테일'].includes(name.trim())
        ) {
            return name.trim();
        }
    }

    return null;
}

// QR 주소에서 상품명 조회
router.get('/lookup', async (req, res) => {
    const qrUrl = req.query.url;

    if (!qrUrl) {
        return res.status(400).json({
            success: false,
            message: 'QR 주소가 필요합니다.'
        });
    }

    try {
        const parsedUrl = new URL(qrUrl);

        if (
            parsedUrl.protocol !== 'https:' ||
            parsedUrl.hostname !== 'hpimg.gsretail.com'
        ) {
            return res.status(400).json({
                success: false,
                message: '지원하지 않는 QR 주소입니다.'
            });
        }

        const response = await fetch(parsedUrl.toString(), {
            headers: {
                'User-Agent': 'Mozilla/5.0'
            },
            signal: AbortSignal.timeout(10000)
        });

        if (!response.ok) {
            throw new Error(
                `웹페이지 접속 실패: ${response.status}`
            );
        }

        const html = await response.text();
        const productName = extractProductName(html);

        console.log('조회된 상품명:', productName);

        res.json({
            success: true,
            product_name: productName
        });

    } catch (err) {
        console.error('상품명 조회 실패:', err.message);

        res.status(500).json({
            success: false,
            product_name: null,
            message: '상품 정보를 가져오지 못했습니다.'
        });
    }
});

// 물품 등록
router.post('/', (req, res) => {
    const {
        product_name,
        category,
        quantity,
        expiry_date,
        ownership_type
    } = req.body;

    if (!['FOOD', 'SUPPLY'].includes(category)) {
        return res.status(400).json({
            success: false,
            message: '상품 분류는 FOOD 또는 SUPPLY여야 합니다.'
        });
    }

    if (!['PERSONAL', 'SHARED'].includes(ownership_type)) {
        return res.status(400).json({
            success: false,
            message: '개인 또는 공용을 선택해야 합니다.'
        });
    }

    if (!product_name || !product_name.trim()) {
        return res.status(400).json({
            success: false,
            message: '상품명을 입력해 주세요.'
        });
    }

    if (!Number.isInteger(Number(quantity)) || Number(quantity) <= 0) {
        return res.status(400).json({
            success: false,
            message: '수량은 1 이상의 숫자여야 합니다.'
        });
    }

    // 유통기한은 선택사항
    // 빈 문자열 또는 null이면 DB에 NULL 저장
    const finalExpiryDate =
        category === 'FOOD' && expiry_date
            ? expiry_date
            : null;

    // 날짜 형식 확인
    if (finalExpiryDate !== null) {
        if (
            typeof finalExpiryDate !== 'string' ||
            !/^\d{4}-\d{2}-\d{2}$/.test(finalExpiryDate)
        ) {
            return res.status(400).json({
                success: false,
                message: '유통기한은 YYYY-MM-DD 형식이어야 합니다.'
            });
        }

        const date = new Date(`${finalExpiryDate}T00:00:00Z`);

        if (
            Number.isNaN(date.getTime()) ||
            date.toISOString().slice(0, 10) !== finalExpiryDate
        ) {
            return res.status(400).json({
                success: false,
                message: '올바른 유통기한을 입력해 주세요.'
            });
        }
    }

    const sql = `
        INSERT INTO items
        (product_name, category, quantity, expiry_date, ownership_type)
        VALUES (?, ?, ?, ?, ?)
    `;

    db.query(
        sql,
        [
            product_name.trim(),
            category,
            Number(quantity),
            finalExpiryDate,
            ownership_type
        ],
        (err, result) => {
            if (err) {
                console.error('물품 등록 DB 오류:', err);

                return res.status(500).json({
                    success: false,
                    message: '물품 등록에 실패했습니다.'
                });
            }

            res.status(201).json({
                success: true,
                message: '물품 등록 성공!',
                item_id: result.insertId
            });
        }
    );
});

module.exports = router;