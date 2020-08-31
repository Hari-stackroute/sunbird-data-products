

import org.apache.spark.sql.{Encoders, SaveMode, SparkSession}
import org.apache.spark.storage.StorageLevel

case class Group(id: String, activities: Set[Map[String, String]], createdby: String, createdon: java.sql.Timestamp, description: Option[String],
                 membershiptype: String, name: String, status: String, updatedby: Option[String], updatedon: java.sql.Timestamp)

case class UserGroup(userid: String, groupid: Set[String])

case class GroupMember(groupid: String, role: String, userid: String, createdby: String, createdon: java.sql.Timestamp, removedby: String, removedon: java.sql.Timestamp,
                       status: Boolean, updatedby: Option[String], updatedon: java.sql.Timestamp)

object GroupsServiceTablesMigration extends Serializable {
    def main(cassandraHost: String): Unit = {
        implicit val spark: SparkSession =
            SparkSession
                .builder()
                .appName("GroupsServiceTablesMigration")
                .config("spark.master", "local[*]")
                .config("spark.cassandra.connection.host", cassandraHost)
                .config("spark.cassandra.output.batch.size.rows", "10000")
                .config("spark.cassandra.read.timeoutMS", "60000")
                //.config("spark.cassandra.input.consistency.level", "ONE")
                .getOrCreate()
        val res = time(migrateGroupsData());
        Console.println("Time taken to execute script", res._1);
        spark.stop();
    }
    def migrateGroupsData()(implicit spark: SparkSession) {
        val groupRes = time(migrateGroup());
        Console.println("Time taken to execute migrateGroupsData script", groupRes._1);
        
       /* val userGroupRes = time(migrateUserGroup());
        Console.println("Time taken to execute migrateGroupsData script", userGroupRes._1);
        
        val res = time(migrateGroupMember());
        Console.println("Time taken to execute migrateGroupsData script", res._1);*/
    }
    
    def migrateGroup() (implicit spark: SparkSession): Unit = {
        val groupSchema = Encoders.product[Group].schema
        val sunbirdGroup = spark.read.format("org.apache.spark.sql.cassandra").schema(groupSchema).option("keyspace", "sunbird").option("table", "group").load().persist(StorageLevel.MEMORY_ONLY)
        println("user_org data Count : " + sunbirdGroup.count())
        sunbirdGroup.write.format("org.apache.spark.sql.cassandra").option("keyspace", "stagingsunbird").option("table", "group").mode(SaveMode.Append).save()
        println("Group count post migration: " + sunbirdGroup.count())
    }
    
    def migrateUserGroup() (implicit spark: SparkSession): Unit = {
        val userGroupSchema = Encoders.product[UserGroup].schema
        val sunbirdUserGroup = spark.read.format("org.apache.spark.sql.cassandra").schema(userGroupSchema).option("keyspace", "sunbird").option("table", "user_group").load().persist(StorageLevel.MEMORY_ONLY)
        println("user_org data Count : " + sunbirdUserGroup.count())
        sunbirdUserGroup.write.format("org.apache.spark.sql.cassandra").option("keyspace", "stagingsunbird").option("table", "user_group").mode(SaveMode.Append).save()
        println("User-group count post migration: " + sunbirdUserGroup.count())
    }
    
    def migrateGroupMember() (implicit spark: SparkSession): Unit = {
        val groupMemberSchema = Encoders.product[UserGroup].schema
        val sunbirdGroupMember = spark.read.format("org.apache.spark.sql.cassandra").schema(groupMemberSchema).option("keyspace", "sunbird").option("table", "group_member").load().persist(StorageLevel.MEMORY_ONLY)
        println("user_org data Count : " + sunbirdGroupMember.count())
        sunbirdGroupMember.write.format("org.apache.spark.sql.cassandra").option("keyspace", "stagingsunbird").option("table", "group_member").mode(SaveMode.Append).save()
        println("Group-member count post migration: " + sunbirdGroupMember.count())
    }
    
    def time[R](block: => R): (Long, R) = {
        val t0 = System.currentTimeMillis()
        val result = block // call-by-name
        val t1 = System.currentTimeMillis()
        ((t1 - t0), result)
    }
}