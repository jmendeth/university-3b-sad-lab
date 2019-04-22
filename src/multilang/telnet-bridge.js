const net = require('net')
const readline = require('readline')
const { TelnetServerSocket, LINEMODE, ECHO } = require('./telnet')

const args = process.argv.slice(2)
if (args.length < 1 || args.length > 2) {
  console.error('Usage: client.js <hostname> [<port>]')
  process.exit(1)
}

net.createServer({ allowHalfOpen: true }, async connection => {
  connection.setKeepAlive(true, 30e3)
  const telnet = new TelnetServerSocket(connection), input = telnet, output = telnet
  let terminal = false

  connection.on("error", err => console.error("Unhandled error on connection:", err.stack || err))
  telnet.on("error", err => console.error("Unhandled error on telnet:", err.stack || err))

  // Negotiate LINEMODE if possible
  if (await telnet.negotiate(LINEMODE, true, 1000).catch(() => false)) {
    // client supports LINEMODE, enable local line editing
    telnet.sendSuboption(LINEMODE, [ 0x01, 1 ])
  } else {
    // client doesn't support LINEMODE, we'll do the editing ourselves
    telnet.announce(ECHO)
    terminal = true
  }

  const socket = net.connect(args[1] || 3500, args[0])
  socket.on("error", err => console.error("Unhandled error on socket:", err.stack || err))
  socket.on('connect', () => {
    const rl = readline.createInterface({
      input, output, terminal, prompt: '\033[1m> '
    })
    const src = readline.createInterface({ input: socket })
    output.write("Connected to server.\r\n")

    src.on('line', line => {
      const wrap = (s, w) => s.replace(new RegExp(`(?![^\\n]{1,${w}}$)([^\\n]{1,${w}})\\s`, 'g'), '$1\n')
      const lines = wrap(line, (output.columns || 80) - 2).split("\n")
      const L = lines.length, CSI = '\u001B[', NLS = '\r\n'.repeat(L)
      output.write(`${CSI}s${CSI}m${NLS}${CSI}${L}A${CSI}${L}L` + lines.join('\r\n  ') + '\r\n' + `${CSI}u${CSI}${L}B${CSI}1m`)
    })
    rl.on('line', line => {
      if (line === ':q') return rl.close()
      if (line.length) socket.write(line + '\n')
      rl.prompt()
    })
    rl.on('close', () => {
      socket.end()
    })
    src.on('close', () => {
      rl.close()
      output.write('\r\n\033[mConnection ended by server.\r\n')
      output.end()
    })

    rl.question('Enter your nickname: ', nick => {
      socket.write(nick + '\n')
      output.write('Type :q to exit\r\n')
      rl.prompt()
    })
  })
}).listen(3333)
