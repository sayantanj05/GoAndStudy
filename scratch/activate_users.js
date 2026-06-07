const { MongoClient } = require('mongodb');

async function main() {
    const uri = "mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster";
    const client = new MongoClient(uri);

    try {
        await client.connect();
        const db = client.db('goandstudydb');

        // Activate specific members
        const result = await db.collection('members').updateMany(
            { email: /sayantan/i },
            { $set: { isActive: true } }
        );
        console.log(`Updated ${result.modifiedCount} members to active.`);

        // Ensure Admin and Staff are active
        const staffResult = await db.collection('staff').updateMany(
            { },
            { $set: { isActive: true } }
        );
        console.log(`Updated ${staffResult.modifiedCount} staff members to active.`);

    } finally {
        await client.close();
    }
}

main().catch(console.error);
