
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const db = require('./config/db');
const itemsRouter = require('./routes/items');

const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json());

app.get('/', (req, res) => {
    res.send('Roomade Server is running!');
});

// MySQL 연결 테스트
app.get('/db-test', (req, res) => {
    db.query('SELECT 1 AS result', (err, results) => {
        if (err) {
            console.error(err);
            return res.status(500).json({
                success: false,
                message: '데이터베이스 연결 실패'
            });
        }

        res.json({
            success: true,
            message: 'Roomade 데이터베이스 연결 성공!',
            data: results
        });
    });
});

// 물품 API 연결
app.use('/items', itemsRouter);

app.listen(PORT, () => {
    console.log(`Roomade Server running on port ${PORT}`);
});
