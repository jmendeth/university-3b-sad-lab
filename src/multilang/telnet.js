const { Transform, Duplex } = require('stream')

// TELNET constants

const IAC = 0xFF
/* commands */
const WILL = 0xFB, WONT = 0xFC, DO = 0xFD, DONT = 0xFE, SB = 0xFA, SE = 0xF0
/* options */
const ECHO = 0x01, LINEMODE = 0x22, NAWS = 0x1f

// TELNET low-level encoding logic

const telnet_data = (data) => {
  const output = []
  data.forEach(x => {
    if (x === IAC) output.push(IAC)
    output.push(x)
  })
  return Buffer.from(output)
}
const telnet_DO = (option) => Buffer.from([ IAC, DO, option ])
const telnet_DONT = (option) => Buffer.from([ IAC, DONT, option ])
const telnet_WILL = (option) => Buffer.from([ IAC, WILL, option ])
const telnet_WONT = (option) => Buffer.from([ IAC, WONT, option ])
const telnet_suboption = (option, data) => Buffer.concat([
  Buffer.from([ IAC, SB, option]), telnet_data(data), Buffer.from([ IAC, SE ]) ])

// TELNET low-level decoder

class TelnetDecoder extends Transform {
  //commmand
  //suboption

  constructor(options) {
    options = options || {}
    options.readableObjectMode = true
    super(options)
    this.dataChunk = []
  }

  flushData() {
    if (this.dataChunk.length)
      this.push({ type: 'data', data: Buffer.from(this.dataChunk) })
    this.dataChunk = []
  }

  processData(x) {
    (this.suboption || this.dataChunk).push(x)
  }

  processCommand(command) {
    if (command[0] === IAC)
      return this.processData(IAC)
    this.flushData()
    if (command[0] === SB) {
      this.suboption = []
    } else if (command[0] === SE) {
      if (this.suboption && this.suboption.length >= 1)
        this.push({ type: 'suboption', option: this.suboption[0], data: Buffer.from(this.suboption.slice(1)) })
      delete this.suboption
    } else {
      this.push({ type: 'command', command: Buffer.from(command) })
    }
  }

  _transform(chunk, encoding, done) {
    const commandLength = x => (x >= 251 && x <= 254) ? 2 : 1
    chunk.forEach(x => {
      if (this.command) {
        // Command state
        this.command.push(x)
        if (this.command.length >= commandLength(this.command[0])) {
          this.processCommand(this.command)
          delete this.command
        }
      } else {
        // Data state
        if (x === IAC)
          this.command = []
        else
          this.processData(x)
      }
    })
    this.flushData()
    done()
  }
}

// TELNET high-level socket wrapper for servers

class TelnetServerSocket extends Duplex {
  //socket
  //decoder

  //columns
  //rows

  constructor(socket, options) {
    super(options)
    this.options = options || {}
    this.socket = socket
    this.decoder = socket.pipe(new TelnetDecoder(this.options.decoderOptions))
    this.decoder.on('data', chunk => this._processChunk(chunk))
    this.decoder.on('end', () => this.push(null))
    //this.decoder.pause()

    this.localNegotiations = new Map()
    this.remoteNegotiations = new Map()
    this.on('command', command => {
      if (!(command[0] === WILL || command[0] === WONT)) return
      this.remoteNegotiations.set(command[1], command[0] === WILL)
      this.emit('negotiation', command[1], command[0] === WILL)
    })
    this.on('suboption', (option, data) => {
      if (option !== NAWS || data.length !== 4) return
      this.columns = data.readUInt16BE(0)
      this.rows = data.readUInt16BE(2)
      this.emit('resize')
    })
    if (this.options.naws || this.options.naws === undefined)
      this.request(NAWS)
  }

  _destroy(err, callback) {
    this.socket.destroy(err)
  }

  _read(size) {
    this.decoder.resume()
  }

  _processChunk(data) {
    if (data.type === 'data') {
      if (!this.push(data.data))
        this.decoder.pause()
    } else if (data.type === 'command') {
      this.emit('command', data.command)
      if (this.options.emitEOF || this.options.emitEOF === undefined) {
        if (data.command[0] === 0xEC) this.push(null) // FIXME
      }
    } else if (data.type === 'suboption') {
      this.emit('suboption', data.option, data.data)
    }
  }

  _write(chunk, encoding, callback) {
    return this.socket.write(telnet_data(chunk), callback)
  }

  _final(callback) {
    return this.socket.end(callback)
  }

  sendSuboption(option, data, callback) {
    return this.socket.write(telnet_suboption(option, data), callback)
  }

  announce(option, enable, callback) {
    if (enable === undefined) enable = true
    return this.socket.write(Buffer.from([ IAC, enable ? WILL : WONT, option ]), callback)
  }

  request(option, enable, force, callback) {
    if (enable === undefined) enable = true
    if (!force && this.localNegotiations.has(option) && this.localNegotiations.get(option) === enable)
      return
    const command = Buffer.from([ IAC, enable ? DO : DONT, option ])
    this.localNegotiations.set(option, enable)
    return this.socket.write(command, callback)
  }

  waitForOption(option, enable, timeout, force) {
    if (enable === undefined) enable = true
    if (!force && this.remoteNegotiations.has(option) && this.remoteNegotiatins.get(option) === enable)
      return Promise.resolve(this.remoteNegotiatins.get(option))
    return new Promise((resolve, reject) => {
      const listener = (soption, supported) => {
        if (soption === option) {
          resolve(supported)
          this.removeListener('command', listener)
        }
      }
      this.on('negotiation', listener)
      if (timeout >= 0) setTimeout(reject, timeout)
    })
  }

  negotiate(option, enable, timeout) {
    this.request(option, enable)
    return this.waitForOption(option, enable, timeout)
  }
}

// Exports
module.exports = {
  IAC, WILL, WONT, DO, DONT, SB, SE, LINEMODE, ECHO, NAWS,
  telnet_data, telnet_WILL, telnet_WONT, telnet_DO, telnet_DONT, telnet_suboption,
  TelnetDecoder,
  TelnetServerSocket,
}
