const {
    Connection,
    PublicKey,
    Transaction,
    TransactionInstruction
  } = require("@solana/web3.js");
  const SPLToken = require("@solana/spl-token");
  
  const dest = "FzGdHWdEq6VnSNmHYMzU1TPfb4gcWUbu6Vbdbi161TfK"; // destination of the server (listener)
  
  const SynexToken = "FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ";
  
  const input = document.getElementById("amount");
  const input2 = document.getElementById("id");
  const input3 = document.getElementById("username");
  const submit = document.getElementById("continue");
  
  const log = document.getElementById("summary");
  
  let price = 0;
  let userid;
  let tokenAmount = 0;
  
  setInterval(async () => {
    price = await (
      await fetch(
        "https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd"
      )
    ).json();
  }, 30000);
  
  const fetchPriceFirst = async () => {
    price = await (
      await fetch(
        "https://api.coingecko.com/api/v3/simple/token_price/solana?contract_addresses=FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ&vs_currencies=usd"
      )
    ).json();
  };
  fetchPriceFirst();
  
  input.addEventListener("input", updateValue);
  input2.addEventListener("input", updateValue);
  input3.addEventListener("input", updateValue);
  
  submit.addEventListener("click", submitFunction);
  
  async function updateValue() {
    let amount = document.getElementById("amount").value || 0;
    tokenAmount = amount;
    let id = document.getElementById("id").value || "Server";
    let username = document.getElementById("username").value || "Player";
    log.textContent = `Importing ${amount} MINECRAFT () to ${id}`;
    log.textContent = `Importing ${amount} MINECRAFT ($${(
      price.FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ.usd * amount
    ).toFixed(2)}) to the server ${id} as ${username}`;
  }
  
  async function submitFunction() {
    let amount = Number(document.getElementById("amount").value);
    let id = document.getElementById("id").value;
    let username = document.getElementById("username").value;
  
    if (id) {
      let serverstatus = (
        await (await fetch(`https://api.mcsrvstat.us/2/${id}`)).json()
      ).online;
      if (serverstatus === true) {
        if (Math.sign(amount) !== -1 && Math.sign(amount) !== 0) {
          if (username) {
            let useruuid = await (
              await fetch(`https://api.minetools.eu/uuid/${username}`)
            ).json();
            userid = useruuid.id;
            if (useruuid.status === "OK") {
              getProvider();
            } else {
              Swal.fire({
                icon: "error",
                title: "Oops...",
                text:
                  'An invalid username was provided! Please enter the username of your account (your player name in Minecraft) in the "Minecraft Username" field'
                //footer: '<a href="">Why do I have this issue?</a>' // Added when a tutorial is out
              });
            }
          } else {
            Swal.fire({
              icon: "error",
              title: "Oops...",
              text:
                'An invalid username was provided! Please enter the username of your account (your player name in Minecraft) in the "Minecraft Username" field'
              //footer: '<a href="">Why do I have this issue?</a>' // Added when a tutorial is out
            });
          }
        } else {
          Swal.fire({
            icon: "error",
            title: "Oops...",
            text: 'Please specify a value greater than 0 for the "Amount" field'
            //footer: '<a href="">Why do I have this issue?</a>' // Added when a tutorial is out
          });
        }
      } else {
        Swal.fire({
          icon: "error",
          title: "Oops...",
          text:
            'Please provide a valid server ip in the "Server IP" field (for example, hypixel.net). This is the IP you connect to with Minecraft. The server might be down.'
          //footer: '<a href="">Why do I have this issue?</a>' // Added when a tutorial is out
        });
      }
    } else {
      Swal.fire({
        icon: "error",
        title: "Oops...",
        text:
          'Please provide a valid server ip in the "Server IP" field (for example, hypixel.net). This is the IP you connect to with Minecraft'
        //footer: '<a href="">Why do I have this issue?</a>' // Added when a tutorial is out
      });
    }
  }
  
  const getProvider = async () => {
    const isPhantomInstalled = window.solana && window.solana.isPhantom;
    if (isPhantomInstalled) {
      try {
        await window.solana.connect();
  
        console.log("Connected to Phantom");
        let pubkey = new PublicKey(window.solana.publicKey.toString());
        const network = "https://api.mainnet-beta.solana.com";
        const connection = new Connection(network);
        const decimals = (
          await connection.getTokenSupply(new PublicKey(SynexToken))
        ).value.decimals;
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
  
        const zeros = ["1"];
  
        for (let i = decimals; i > 0; i--) {
          zeros.push("0");
        }
        let ata = await getTokenAccount(pubkey);
        const decimalplace = Number(zeros.join(""));
        const transaction = new Transaction().add(
          SPLToken.Token.createTransferInstruction(
            SPLToken.TOKEN_PROGRAM_ID, // always token program address
            new PublicKey(ata), // from (token account public key)
            new PublicKey(dest), // to (token account public key)
            pubkey, // from's authority
            [], // pass signer if from's mint is a multisig pubkey
            tokenAmount * decimalplace // amount
          )
        );
        transaction.feePayer = window.solana.publicKey;
        let blockhashObj = await connection.getRecentBlockhash();
        transaction.recentBlockhash = await blockhashObj.blockhash;
        let uuid =
          userid.substr(0, 8) +
          "-" +
          userid.substr(8, 4) +
          "-" +
          userid.substr(12, 4) +
          "-" +
          userid.substr(16, 4) +
          "-" +
          userid.substr(20);
        transaction.add(
          new TransactionInstruction({
            keys: [
              {
                pubkey: window.solana.publicKey,
                isSigner: true,
                isWritable: true
              }
            ],
            data: Buffer.from(uuid, "utf-8"),
            programId: new PublicKey(
              "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"
            )
          })
        );
        let signed = await window.solana.signTransaction(transaction);
        let signature = await connection.sendRawTransaction(signed.serialize());
        Swal.fire({
          icon: "success",
          title: "Transaction Sent",
          text: "The transaction was sent on the Blockchain!",
          footer: `<a href='https://solscan.io/tx/${signature}' target="_blank">View on Blockchain Explorer</a>`
        });
        await connection.confirmTransaction(signature);
        Swal.fire({
          icon: "success",
          title: "Transaction Confirmed",
          text:
            "The transaction was confirmed. You should get your items shortly!",
          footer: `<a href='https://solscan.io/tx/${signature}' target="_blank">View on Blockchain Explorer</a>`
        });
        console.log("Sent transaction");
      } catch (err) {
        console.log(err);
        // { code: 4001, message: 'User rejected the request.' }
      }
    } else {
      console.log("Phantom was not detected");
    }
  };
  