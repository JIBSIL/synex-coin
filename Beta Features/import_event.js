const web3 = require("@solana/web3.js");
const SPLToken = require('@solana/spl-token');

let fs = require('fs');

const SynexToken = 'FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ'
const usdcContact = 'EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v'
const walletAddress = 'solanaAddressToListenTo' // address to listen for transactions on
const file = 'test.txt' // file to write to

let output = []

let i = 0

process.on('uncaughtException', function (err) {
  console.log((new Date).toUTCString() + ' uncaughtException:', err.message)
  console.log(err.stack)
})

const readOrWriteIfEmpty = () => {
  return new Promise((resolve) => {
    fs.readFile(file, 'utf8', (err, data) => {
      if (!err) {
        resolve(data)
      } else if (JSON.stringify(err).includes('ENOENT')) {
        fs.writeFileSync(file, '{}')
        resolve('{}')
      }
    });
  })
}

const getTx = async () => {
  const fetch = await import('got')
  async function getTokenAccount(publicKey, mint) {
    return new Promise(async (resolve, reject) => {
      let tokenAccountRaw = await SPLToken.Token.getAssociatedTokenAddress(
        SPLToken.ASSOCIATED_TOKEN_PROGRAM_ID, // always associated token program id
        SPLToken.TOKEN_PROGRAM_ID, // always token program id
        new web3.PublicKey(mint), // mint
        publicKey // token account authority
      );
      //console.log(tokenAccountRaw)
      //let tokenAccountReturn = tokenAccountRaw.toBase58();

      resolve(tokenAccountRaw);
    });
  }
  price = (await fetch.default.get('https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd').json()).FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ.usd
  setInterval(async () => {
    let priceraw = (await fetch.default.get('https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd').json())
    price = priceraw.FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ.usd
  }, 30000)
  const data = JSON.parse(await readOrWriteIfEmpty()) // Import data from file
  const solana = new web3.Connection("https://ssc-dao.genesysgo.net");
  const synex = await solana.getSignaturesForAddress(await getTokenAccount(new web3.PublicKey(walletAddress), SynexToken))
  const usdc = await solana.getSignaturesForAddress(await getTokenAccount(new web3.PublicKey(walletAddress), usdcContact))
  let alltx = synex.concat(usdc)
  alltx.map(async (res) => {
    if (!data[`SYNEX${res.signature}`]) {
      data[`SYNEX${res.signature}`] = { // Add "SYNEX" to the start because numbers at the start of vars aren't allowed
        memo: res.memo,
        datetime: new Date(res.blockTime * 1000).toString()
      }
      console.log('Processing Transaction with hash ' + res.signature)
      let tx = await solana.getTransaction(res.signature)
      if (tx.meta.postTokenBalances[0] && tx.meta.preTokenBalances[0] && tx.meta.postTokenBalances[1] && tx.meta.preTokenBalances[1]) {
        // prevents "properties of undefined" error
        let amount1 = tx.meta.postTokenBalances[0].uiTokenAmount.uiAmount - tx.meta.preTokenBalances[0].uiTokenAmount.uiAmount
        let amount2 = tx.meta.postTokenBalances[1].uiTokenAmount.uiAmount - tx.meta.preTokenBalances[1].uiTokenAmount.uiAmount
        let amount = (amount1 > amount2) ? amount1 : amount2
        tx.meta.logMessages.map((msg) => {
          if (new RegExp(/Memo \(len ([0-9])*\): "(.)*"/).test(msg) === true) {
            let memo = new RegExp(/[^"]+/).exec(new RegExp(/"(.)*"/).exec(msg)[0])[0]
            if (tx.meta.preTokenBalances[0].mint === tx.meta.postTokenBalances[0].mint && tx.meta.preTokenBalances[0].mint === SynexToken) {
              console.log(`Crediting ${amount} MINECRAFT ($${amount * price}) tokens to the account ${memo}`)
              output.push(`Credit,${amount},${memo}`)
            } else if (tx.meta.preTokenBalances[0].mint === tx.meta.postTokenBalances[0].mint && tx.meta.preTokenBalances[0].mint === usdcContact) {
              console.log(`Crediting ${amount / price} MINECRAFT tokens ($${amount}) to the account ${memo}`)
              output.push(`Credit,${amount / price},${memo}`)
            }
          }
        })
        i++
      }
    }
  })
  if (i !== 0) {
    console.log(`+ ${i} entries added`)
  }
  i = 0
  fs.writeFileSync(file, JSON.stringify(data))
}

setInterval(() => {
  try {
    getTx()
  } catch (error) {
    console.log(error)
  }
}, 5000)

setInterval(() => {
  console.log(output)
}, 15000)