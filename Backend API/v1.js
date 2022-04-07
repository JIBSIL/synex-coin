const {
    Keypair,
    Connection,
    PublicKey,
    Transaction,
  } = require('@solana/web3.js');
  const SPLToken = require('@solana/spl-token');
  const { derivePath } = require('ed25519-hd-key');
  const bip39 = require('bip39');
  const SynexToken = 'FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ'; // Synex token address
  
  async function sendMoney(destRaw, amount) {
    return new Promise(async (resolve, reject) => {
      try {
        let seedFromPhantomWallet =
          'phantomSeed';
        let path = `m/44'/501'/0'/0'`; // Default phantom seed
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
        resolve(txsend);
      } catch (error) {
        console.log(error);
      }
    });
  }
  
  const main = async () => {
    console.log(await sendMoney('solanaAddressWithTokenAccount', 1));
  };
  main();
  