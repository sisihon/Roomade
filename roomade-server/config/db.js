
const mysql = require('mysql2');

const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

db.getConnection((err, connection) => {
    if (err) {
        console.error('MySQL 연결 실패:', err.message);
        return;
    }

    console.log('MySQL 데이터베이스 연결 성공!');
    connection.release();
});

module.exports = db;
