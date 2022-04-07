// Require
const { MongoClient } = require('mongodb');
const uri = "mongoConnectionString";
const client = new MongoClient(uri, { useNewUrlParser: true, useUnifiedTopology: true });
// End require

const db = client.db('db');
const collection = db.collection("dev");

// Set up Transfer logic with Mongodb

// Loop alerts

const internalTx = (from, to, rawAmount) => {
    return new Promise(async (resolve, reject) => {

        const amount = Math.floor(rawAmount)

        if(Math.sign(amount) === -1) {
            resolve('Error: Negative numbers are not allowed!')
        }

        let findOneTest = await collection.findOne({
            type: 'user',
            uuid: to
        })

        let findTwoTest = await collection.findOne({
            type: 'user',
            uuid: from
        })

        if(!findOneTest) {
            await collection.insertOne({
                type: 'user',
                uuid: to,
                balance: 0
            });
        }

        if(!findTwoTest) {
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

        let findTwo = await collection.findOne({
            type: 'user',
            uuid: from
        })

        if(findTwo.balance - amount < 0) {
            resolve('Error: Not enough balance')
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

        resolve(`Transferred ${amount} from ${from} to ${to}. The new balance of ${from} is ${findAfter2.balance} The new balance of ${to} is ${findAfter.balance}`)
    })
}

const preload = async () => {
    return new Promise(async (resolve, reject) => {
        await client.connect();
        resolve()
    })
}

const main = async () => {
    await preload()
    const alert = await internalTx('Server', 'SynexUser', 1) // Create an internal tx (database)
    console.log(alert)
    process.exit(1)
}; main()