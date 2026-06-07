const { MongoClient } = require('mongodb');

async function main() {
    const uri = "mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster";
    const client = new MongoClient(uri);

    try {
        await client.connect();
        const db = client.db('goandstudydb');

        console.log("--- ADMINS ---");
        const admins = await db.collection('admins').find({}).toArray();
        admins.forEach(a => console.log(`${a.email}: active=${a.isActive}, role=${a.role}, pass=${a.passwordHash}`));

        console.log("\n--- STAFF ---");
        const staff = await db.collection('staff').find({}).toArray();
        staff.forEach(s => console.log(`${s.email}: active=${s.isActive}, role=${s.role}, pass=${s.passwordHash}`));

        console.log("\n--- MEMBERS ---");
        const members = await db.collection('members').find({email: /@/}).toArray();
        members.forEach(m => console.log(`${m.email}: active=${m.isActive}, role=${m.role}`));

    } finally {
        await client.close();
    }
}

main().catch(console.error);
