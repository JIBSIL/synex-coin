// Require
const { MongoClient } = require('mongodb');
const uri = "mongoConnectionString";
const client = new MongoClient(uri, { useNewUrlParser: true, useUnifiedTopology: true });
// End require

const user = 'GithubUser'
const db = client.db('yourMongoDB');
const collection = db.collection("mongoCollection");

// Set up Transfer logic with Mongodb

// Loop alerts

const internalTx = (from, to, amount) => {
    return new Promise(async (resolve, reject) => {

        const findOneTest = await collection.findOne({
            type: 'user',
            uuid: to
        })

        if(!findOneTest) {
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

        resolve(`Updated, new balance: ${findAfter.balance}`)
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
    const alert = await internalTx('UCALLER', 'GithubUser', 100)
    console.log(alert)
    process.exit(1)
}; main()