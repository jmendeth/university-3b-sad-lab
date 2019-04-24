const net = require('net')
const readline = require('readline')

const args = process.argv.slice(2)
if (args.length < 1 || args.length > 2) {
  console.error('Usage: client.js <hostname> [<port>]')
  process.exit(1)
}

const input = process.stdin, output = process.stdout, terminal = true
const socket = net.connect(args[1] || 3500, args[0])
socket.on('connect', () => {
  const rl = readline.createInterface({
    input, output, terminal, prompt: '\033[1m> '
  })
  const src = readline.createInterface({ input: socket })
  output.write('Connected to server.\n')

  src.on('line', line => {
    output.write('\033[s\033[m\n\033[A\033[L' + line + '\033[u\033[B\033[1m')
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
    output.write('\n\033[mConnection ended by server.\n')
  })

  rl.question('Enter your nickname: ', nick => {
    socket.write(nick + '\n')
    output.write('Type :q to exit\n')
    rl.prompt()
  })
})
