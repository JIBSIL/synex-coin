# synex-coin
Synex is a Solana utility token for buying, trading, and integrating crypto into Minecraft.

## Why you should use Synex as a user
- Convert your hard-earned ingame cash into real crypto
- An easy way to get into crypto as a gamer
- No learning curve (use Synex just as you would use the ingame currency)

## Why you should integrate Synex into your server
- Turn season payouts into player-specific rewards
- Fund your economy with as little or as much crypto as you want
- Attract players who are interested in crypto (boost playercount)
- Boost donations with an optional crypto-to-ingame currency exchanger built in

Telegram: https://t.me/synexchat

Website: https://synexcoin.dev

## Using Synex as a player:
1. Make some ingame money by selling items, getting credits for playtime or whatever the server owner chooses to implement
2. Create a Solana wallet. Officially recommended browser wallets include Phantom and Solflare
3. Have the server owner create a token account for you, usually by sending a small amount of MINECRAFT coin to your wallet to initialize your account. This can be automated or manual, depending on the server. *Auto wallet creation will be added soon to the API for owners to implement*
4. Check the exchange rate of **ingame crypto** : **real crypto** and export the amount you would like using the /crypto export command.
5. The crypto should be sent to your wallet. 
*If the crypto is not sent, it should be refunded back to your wallet for you to try again. If it's not, it is likely a problem on the server owner's side and should be refunded by staff members (as the crypto never left the owner's wallet).*

## Using Synex as a server:
### We know that most server owners don't want to run their own version of the API (which requires a working Linux environment) and will be happy to host it for anyone, for free! Just contact us here and we'll get you set up: https://t.me/synexchat
1. Get a version of the API. Currently, we let server owners request a grant of tokens and a live version of the API to use, hosted by us. This will be a web address and an API key, for example: https://api.synexcoin.dev/apiKey (but you're welcome to use your own domain!)
2. Make a Synex Coin token account using Phantom or Solflare. You may get free tokens by applying for a grant: message @JIBSIL on telegram.
3. Install Skript, SkQuery, SkBee and Reqn (dependencies) as well as WebEcon and crypto.sk (built in-house by us). See the FAQ for info on exactly what these do.
4. Place crypto.sk in the `plugins/Skript/scripts` folder and run the command `sk reload crypto`. This activates the script.
5. Edit crypto.sk's variable to change the exchange rate, to set the API url and to set the Solflare/Phantom address that you set up before.
6. Optionally, enable importing using the guide in the FAQ
7. Finished! If you have any questions open a Github issue or put ask in the chat: https://t.me/synexchat

## FAQ

Wait, but what if I don't want to use Synex as a player on a Synex-supported server?
- You can opt-out of using synex simply by not exporting. This does not require any further steps.
What if I want to import Synex Coin tokens (as a player)?
- You can import tokens by using a web importing tool. This securely connects to your wallet and takes an amount you request, uses the server's exchange rate to convert to ingame currency and credits the tokens to whatever user you choose. You can always export the tokens later. Since this all takes place on a decentralized blockchain, **server owners can not authorize transactions on your behalf** and your coins are safe throughout the importing process.
What if I want to allow importing of tokens (as a server owner)?
- To allow importing, you have to set your wallet you use in the settings of the frontend skript (for example, crypto.sk). You also have to set the wallet address in the backend .js script. You also have to self-host a version of the .html importer webpage for users to import.
*A feature that simplifies this process will be added soon.*
Why do I have to run so many plugins for Synex to work, and what do they all do??
- Synex runs off of a plugin called Skript, which is the language that Synex is coded in. This plugin is **required** for Synex to work.
- SkQuery and SkBee are **optional**. We sometimes use methods from these plugins and plan to use a lot of it in the future. For future-proofing, these are recommended. These are what are known as Skript "addons", or plugins that extend Skript's functionality.
- Reqn is a **required** Skript addon that allows web requests in Skript. This allows the server to talk to the API.
- WebEcon replaces your current economy manager with one that allows for calling requests to a web API.
- Crypto.sk is a **required** script (a plugin made in Skript) that should be placed in the `plugins/Skript/scripts` folder. This allows for the basic functionality of importing, exporting, checking balance, paying and more.
Are these requests that are called by players asynchronous?
- Yes: we use async requests to provide the least performance impact to your server
