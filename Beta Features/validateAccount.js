const web3 = require("@solana/web3.js");
try {
    (async () => {
            try {
                const solana = new web3.Connection("https://ssc-dao.genesysgo.net");
                console.log(await solana.getAccountInfo(new web3.PublicKey('pubkey'))); // get acc info
            } catch (error) {
                console.log(error)
            }
    })();
} catch (error) {
    console.log(error)
}
