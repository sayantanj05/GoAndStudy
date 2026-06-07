require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors({ origin: process.env.CORS_ORIGIN?.split(',') }));

const server = http.createServer(app);
const io = new Server(server, {
  cors: { 
    origin: process.env.CORS_ORIGIN?.split(',') || ['http://localhost:3003', 'http://localhost:3000'], 
    methods: ['GET', 'POST'],
    credentials: true
  }
});

io.on('connection', (socket) => {
  const { role, userId } = socket.handshake.query;

  if (role === 'ROLE_ADMIN') socket.join('admin');
  if (role === 'ROLE_STAFF') socket.join('staff');
  if (role === 'ROLE_MEMBER') socket.join(`member-${userId}`);

  socket.on('disconnect', () => {});
});

app.post('/broadcast', (req, res) => {
  const secret = req.headers['x-realtime-secret'];
  if (secret !== process.env.REALTIME_SECRET) {
    return res.status(403).json({ error: 'Forbidden' });
  }

  const { event, room, data } = req.body;
  if (room === 'all') {
    io.emit(event, data);
  } else {
    io.to(room).emit(event, data);
  }
  res.json({ broadcast: true, event, room });
});

app.get('/health', (req, res) => res.json({ status: 'UP', connections: io.engine.clientsCount }));

server.listen(process.env.PORT || 3002, () => {
  console.log(`GoAndStudy realtime server running on port ${process.env.PORT || 3002}`);
});
