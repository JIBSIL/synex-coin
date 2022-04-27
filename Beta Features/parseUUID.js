// parse uuid with dashes

let uuid = '5231b533ba17478798a3f2df37de2aD7' // minecraft uuid

console.log(uuid.substr(0, 8) + "-" + uuid.substr(8, 4) + "-" + uuid.substr(12, 4) + "-" + uuid.substr(16, 4) + "-" + uuid.substr(20))