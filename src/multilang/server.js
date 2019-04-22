const net = require('net')
const readline = require('readline')

const peers = new Map()
const broadcast = (origin, data) =>
  peers.forEach((socket, nick) => nick !== origin && socket.write(data))

const server = net.createServer(socket => {
  socket.setKeepAlive(true, 30e3)

  const rl = readline.createInterface({ input: socket })
  rl.once('line', nick => {
    if (!/^[a-zA-Z0-9 _.@-]+$/.test(nick))
      return socket.end('Error: Invalid characters in nickname\n')
    if (peers.has(nick))
      return socket.end(`Error: Nickname '${nick}' already in use\n`)

    broadcast(nick, `[${nick} joined the room]\n`)
    peers.set(nick, socket)
    socket.write(`[current participants: ${Array.from(peers.keys()).join(", ")}]\n`)
    rl.on('line', message => broadcast(nick, `${nick}: ${message}\n`))
    rl.on('close', () => {
      peers.delete(nick)
      broadcast(nick, `[${nick} left the room]\n`)
    })
  })

  socket.on('error', () => {
    rl.close()
    socket.destroy()
  })
})
server.listen(3500, () => console.log('Server listening.'))
