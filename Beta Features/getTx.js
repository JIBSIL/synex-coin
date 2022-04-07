const { Connection } = require('@solana/web3.js')

const url = 'https://ssc-dao.genesysgo.net';

const SynexToken = 'FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ'
const usdcContact = 'EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v'
const transactionhash = '23eUSN1sDe3PwGSCu9GkvFkNwCyqUbfANRzkExFQKaBMwZir2NzRvh8Fb8oeF3hWSnxGD2kxxeyffgQQxG5VB2Sk' // Random transaction hash to test for transactions on

const main = async () => {
    const fetch = await import('got')
    price = (await fetch.default.get('https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd').json()).FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ.usd
    setInterval(async () => {
        let priceraw = (await fetch.default.get('https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd').json())
        price = priceraw.FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ.usd
    }, 30000)

    let connection = new Connection(url);

    let tx = await connection.getTransaction(transactionhash)
    let amount = tx.meta.postTokenBalances[0].uiTokenAmount.uiAmount - tx.meta.preTokenBalances[0].uiTokenAmount.uiAmount
    tx.meta.logMessages.map((msg) => {
        if (new RegExp(/Memo \(len ([0-9])*\): "(.)*"/).test(msg) === true) {
            let memo = new RegExp(/[^"]+/).exec(new RegExp(/"(.)*"/).exec(msg)[0])[0]
            if (tx.meta.preTokenBalances[0].mint === tx.meta.postTokenBalances[0].mint && tx.meta.preTokenBalances[0].mint === SynexToken) {
                console.log(`Crediting ${amount} MINECRAFT ($${amount * price}) tokens to the account ${memo}`)
            } else if (tx.meta.preTokenBalances[0].mint === tx.meta.postTokenBalances[0].mint && tx.meta.preTokenBalances[0].mint === usdcContact) {
                console.log(`Crediting ${amount / price} MINECRAFT tokens ($${amount}) to the account ${memo}`)
            }
        }
    })
};

try {
    main()
} catch (error) {
    console.log(error)
}