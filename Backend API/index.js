const express = require('express')
const app = express()
const port = 3000
const {
  Keypair,
  Connection,
  PublicKey,
  Transaction,
} = require('@solana/web3.js');
const SPLToken = require('@solana/spl-token');
const { derivePath } = require('ed25519-hd-key');
const bip39 = require('bip39');
const file = 'transactions.json'
const fs = require('fs')
let output = []
let walletAddress = false
const SynexToken = 'FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ'
const usdcContact = 'EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v'
let monitorstarted = false
let numEntries = 0
let priceMultiple = false

// Require
const { MongoClient } = require('mongodb');
const uri = "mongoConnectionString";
const client = new MongoClient(uri, { useNewUrlParser: true, useUnifiedTopology: true });
// End require

const user = 'mongodbUser'
const db = client.db('mongoDatabase');
const collection = db.collection("dev");

process.on('uncaughtException', function (err) {
  console.log((new Date).toUTCString() + ' uncaughtException:', err.message)
  //console.log(err.stack)
}) // Catch process-ending errors

// Read 'file' var and initialize so that txs aren't repeated multiple times

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

// Set up Transfer logic with Mongodb

const balance = (address) => {
  return new Promise(async (resolve, reject) => {
    let findTwo = await collection.findOne({
      type: 'user',
      uuid: address
    })

    if (findTwo) {
      if (findTwo.balance) {
        resolve(`Success,${findTwo.balance}`)
      } else {
        resolve(`Error,Database Error`)
      }
    } else {
      resolve(`Error,User not found`)
    }
  })
}

// Loop alerts

const setBalance = (address, amount) => {
  return new Promise(async (resolve, reject) => {
    if(Number(Math.sign(amount)) === -1) {
      resolve('Error,Negative numbers are not allowed!')
      return;
    }

    let findOne = await collection.findOne({
      type: 'user',
      uuid: address
    })

    if(findOne) {
      await collection.updateOne(
        {
          type: 'user',
          uuid: address
        },
        {
          $set: {
            balance: Number(amount)
          }
        }
      );
      resolve(`Success,New balance: ${Number(amount)}`)

    } else {
      await collection.insertOne({
        type: 'user',
        uuid: address,
        balance: 0
      });

      await collection.updateOne(
        {
          type: 'user',
          uuid: address
        },
        {
          $set: {
            balance: Number(amount)
          }
        }
      );
      resolve(`Success,New balance: ${Number(amount)}`)
    }
  })
}

const internalTx = (from, to, rawAmount) => {
  return new Promise(async (resolve, reject) => {
    const amount = Math.floor(rawAmount)

    if (Number(Math.sign(amount)) === -1) {
      resolve('Error,Negative numbers are not allowed!')
      return;
    }

    let findOneTest = await collection.findOne({
      type: 'user',
      uuid: to
    })

    let findTwoTest = await collection.findOne({
      type: 'user',
      uuid: from
    })

    if (!findOneTest) {
      await collection.insertOne({
        type: 'user',
        uuid: to,
        balance: 0
      });
    }

    if (!findTwoTest) {
      await collection.insertOne({
        type: 'user',
        uuid: from,
        balance: 0
      });
    }

    let findOne = await collection.findOne({
      type: 'user',
      uuid: to
    })

    let findTwoRaw = await collection.findOne({
      type: 'user',
      uuid: from
    })

    if (findTwoRaw.balance - amount < 0) {
      resolve('Error,Not enough balance')
      return;
    }

    await collection.updateOne(
      {
        type: 'user',
        uuid: to
      },
      {
        $set: {
          balance: findOne.balance + amount
        }
      }
    );

    let findTwo = await collection.findOne({
      type: 'user',
      uuid: from
    })

    await collection.updateOne(
      {
        type: 'user',
        uuid: from
      },
      {
        $set: {
          balance: findTwo.balance - amount
        }
      }
    );

    const findAfter = await collection.findOne({
      type: 'user',
      uuid: to
    })

    const findAfter2 = await collection.findOne({
      type: 'user',
      uuid: from
    })

    resolve(`Success,${amount},${findAfter.balance},${findAfter2.balance}`)
  })
}

const preload = async () => {
  return new Promise(async (resolve, reject) => {
    await client.connect();
    resolve()
  })
}

async function sendMoney(destRaw, amountRaw, from) {
  return new Promise(async (resolve, reject) => {
    try {
      let amount = Number(amountRaw)
      let findTwoRaw = await collection.findOne({
        type: 'user',
        uuid: from
      })

      if (findTwoRaw.balance - amount * exchangeRate < 0) {
        resolve('Error,Not enough balance')
        return;
      }
      let seedFromPhantomWallet =
        'phantomGenerationSeed';
      let path = `m/44'/501'/0'/0'`; // Default Phantom app (phantom.app)
      var seedBuffer = bip39.mnemonicToSeedSync(seedFromPhantomWallet, '');
      let fromPhantomSeed = Keypair.fromSeed(
        derivePath(path, seedBuffer.toString('hex')).key
      );

      const url = 'https://ssc-dao.genesysgo.net';

      let connection = new Connection(url);

      async function getTokenAccount(publicKey) {
        return new Promise(async (resolve, reject) => {
          let tokenAccountRaw = await SPLToken.Token.getAssociatedTokenAddress(
            SPLToken.ASSOCIATED_TOKEN_PROGRAM_ID, // always associated token program id
            SPLToken.TOKEN_PROGRAM_ID, // always token program id
            new PublicKey(SynexToken), // mint
            publicKey // token account authority
          );

          let tokenAccountReturn = tokenAccountRaw.toBase58();

          resolve(tokenAccountReturn);
        });
      }

      // print version
      const lamportBalance = await connection.getBalance(
        fromPhantomSeed.publicKey
      );
      const solBalance = lamportBalance / 10000000000;
      const tokenAccount = await getTokenAccount(fromPhantomSeed.publicKey);
      const dest = await getTokenAccount(new PublicKey(destRaw));
      let tx = new Transaction();
      const decimals = (
        await connection.getTokenSupply(new PublicKey(SynexToken))
      ).value.decimals;
      const zeros = ['1'];

      for (let i = decimals; i > 0; i--) {
        zeros.push('0');
      }

      const decimalplace = Number(zeros.join(''));

      tx.add(
        SPLToken.Token.createTransferInstruction(
          SPLToken.TOKEN_PROGRAM_ID, // always token program address
          new PublicKey(tokenAccount), // from (token account public key)
          new PublicKey(dest), // to (token account public key)
          fromPhantomSeed.publicKey, // from's authority
          [], // pass signer if from's mint is a multisig pubkey
          amount * decimalplace // amount
        )
      );
      tx.feePayer = fromPhantomSeed.publicKey;

      let txsend = await connection.sendTransaction(tx, [fromPhantomSeed]);
      await collection.updateOne(
        {
          type: 'user',
          uuid: from
        },
        {
          $set: {
            balance: findTwoRaw.balance - amount * exchangeRate
          }
        }
      );
      resolve(`Success,${txsend}`);
    } catch (error) {
      resolve(`Error,${error}`);
      console.log(error)
    }
  });
}

const airdrop = (to, amount) => {
  return new Promise(async (resolve, reject) => {

    const findOneTest = await collection.findOne({
      type: 'user',
      uuid: to
    })

    if (!findOneTest) {
      await collection.insertOne({
        type: 'user',
        uuid: to,
        balance: 0
      });
    }

    const findOne = await collection.findOne({
      type: 'user',
      uuid: to
    })
    await collection.updateOne(
      {
        type: 'user',
        uuid: to
      },
      {
        $set: {
          balance: findOne.balance + amount
        }
      }
    );

    const findAfter = await collection.findOne({
      type: 'user',
      uuid: to
    })

    resolve(`Success,${findAfter.balance}`)
  })
}

// Start import logic

const getTx = async () => {
  return new Promise(async (resolve, reject) => {
    async function getTokenAccount(publicKey, mint) {
      return new Promise(async (resolve, reject) => {
        let tokenAccountRaw = await SPLToken.Token.getAssociatedTokenAddress(
          SPLToken.ASSOCIATED_TOKEN_PROGRAM_ID, // always associated token program id
          SPLToken.TOKEN_PROGRAM_ID, // always token program id
          new PublicKey(mint), // mint
          publicKey // token account authority
        );
        //console.log(tokenAccountRaw)
        //let tokenAccountReturn = tokenAccountRaw.toBase58();
  
        resolve(tokenAccountRaw);
      });
    }
    const fetch = await import('got')
    price = (await fetch.default.get('https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd').json()).FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ.usd
    if(!priceMultiple === false) {
      priceMultiple = false
      setInterval(async () => {
        let priceraw = (await fetch.default.get('https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd').json())
        price = priceraw.FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ.usd
      }, 30000)
    }
    const data = JSON.parse(await readOrWriteIfEmpty()) // Import data from file
    const solana = new Connection("https://ssc-dao.genesysgo.net");
    const synex = await solana.getSignaturesForAddress(await getTokenAccount(new PublicKey(walletAddress), SynexToken))
    const usdc = await solana.getSignaturesForAddress(await getTokenAccount(new PublicKey(walletAddress), usdcContact))
    let alltx = synex.concat(usdc)
    alltx.map(async (res) => {
      if (!data[`SYNEX${res.signature}`]) {
        data[`SYNEX${res.signature}`] = { // Add "SYNEX" to the start because numbers at the start of vars aren't allowed
          memo: res.memo,
          datetime: new Date(res.blockTime * 1000).toString()
        }
        // console.log('Processing Transaction with hash ' + res.signature) // For debugging purposes
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
                //console.log(`Crediting ${amount} MINECRAFT ($${amount * price}) tokens to the account ${memo}`)
                output.push(`Credit:${amount}:${memo}`)
              } else if (tx.meta.preTokenBalances[0].mint === tx.meta.postTokenBalances[0].mint && tx.meta.preTokenBalances[0].mint === usdcContact) {
                //console.log(`Crediting ${amount / price} MINECRAFT tokens ($${amount}) to the account ${memo}`)
                output.push(`Credit:${amount / price}:${memo}`)
              }
            }
          })
          numEntries++
        }
      }
    })
    if (numEntries !== 0) {
      //console.log(`+ ${numEntries} entries added`) // For debugging purposes
    }
    numEntries = 0
    fs.writeFileSync(file, JSON.stringify(data))
    resolve()
  })
}

// End import logic

// Start route logic

let secret = '098f6bcd4621d373cade4e832627b4f6' // MD5 hash of 'test'

app.get(`/${secret}/exportTokens/:address/:amount/:from`, async (req, res) => {
  const tx = await sendMoney(req.params.address, req.params.amount, req.params.from)
  res.send(tx)
})

app.get(`/${secret}/send/:from/:to/:amount`, async (req, res) => {
  await preload()
  const alert = await internalTx(req.params.from, req.params.to, req.params.amount)
  console.log(alert)
  res.send(alert)
})

app.get(`/${secret}/airdrop/:to/:amount`, async (req, res) => {
  await preload()
  const alert = await airdrop(req.params.to, Number(req.params.amount))
  res.send(alert)
})

app.get(`${secret}/balance/:address`, async (req, res) => {
  await preload()
  const alert = await balance(req.params.address)
  res.send(alert)
})

app.get(`/${secret}/setBalance/:address/:amount`, async (req, res) => {
  await preload()
  const alert = await setBalance(req.params.address, req.params.amount) 
  res.send(alert)
})

app.get(`/${secret}/import/setWalletAddress/:address/:rate`, async (req, res) => {
  walletAddress = req.params.address
  exchangeRate = Number(req.params.rate)
  res.send('Success,Set')
})

app.get(`/${secret}/import/startMonitor`, async (req, res) => {
  if(monitorstarted !== true) {
    if(!walletAddress === false) {
      console.log('Monitor started')
      monitorstarted = true
      getTx()
      res.send('Success,Started')
      setInterval(() => {
        monitorstarted = true
        getTx()
      }, 10000)
    } else {
      res.send('Error,Set Address First')
    }
  } else {
    res.send('Error,Already running')
  }
})

app.get(`/${secret}/import/readData`, async (req, res) => {
  if(output) {
    if(output.length !== 0) {
      res.send(`Success;${output}`)
    } else {
      res.send('Error;No Transactions')
    }
  } else {
    res.send('Error;Not started')
  }
})

app.get(`/${secret}/import/deleteData`, async (req, res) => {
  if(output) {
    output = []
    res.send(`Success,Cleared`)
  } else {
    res.send('Error,Not started')
  }
})

app.get(`/${secret}/SynexPrice`, async (req, res) => {
  if(synexprice) {
    res.send(`Success,${Number(synexprice).toFixed(4)}`)
  } else {
    res.send('Error,Not started')
  }
})

// End route logic

async function main() {
  const fetch = await import('got')
  await client.connect()
  console.log('Database connection successful!')
  let priceraw = (await fetch.default.get('https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd').json())
  synexprice = priceraw.FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ.usd
  setInterval(async () => {
    let priceraw = (await fetch.default.get('https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd').json())
    synexprice = priceraw.FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ.usd
  }, 30000) 
  app.listen(port, () => {
    console.log(`App listening at http://localhost:${port}`)
  })
}; main()