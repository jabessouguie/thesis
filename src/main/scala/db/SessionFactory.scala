package thesis.db

import org.squeryl.Session
import org.squeryl.SessionFactory
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import org.squeryl.dsl._

object ThesisSession	{
	val dbUser = "thesis"
	val dbPass = "retanner"
	val dbConn = "jdbc:postgresql://sparky.ryantanner.org/thesis"
	
	def startDbSession():Unit = {
        if(!Session.hasCurrentSession)  {
          Class.forName("org.postgresql.Driver")
          SessionFactory.concreteFactory = Some(() => Session.create(
              java.sql.DriverManager.getConnection(dbConn,dbUser,dbPass),
              new PostgreSqlAdapter)
		    )
        }
	}

	def initSchema = {

		startDbSession()

		transaction {
			EntityGraph.create
			println("Created the schema")
		}
	}
	
	def insertDocument(d: thesis.Document): Long = { 
        val ret = EntityGraph.documents.insert(new thesis.db.Document(d.filePath))
        println("Inserted document: " + d.filePath)
        //return retDoc.id
        return ret.id
    }

    def insertSentence(s: thesis.Sentence, dId: Long): Long = {
        val sent = s.tokens.mkString(" ")
        val newS = EntityGraph.sentences.insert(new Sentence(dId, sent))
        return newS.id
    }

    def insertAlias(entityValue: String, representative: Boolean, docId: Long, masterId: Option[Long]): Long = {
        val newEnt = EntityGraph.entities.insert(new Entity(entityValue, None, representative, masterId))
        EntityGraph.entitiesFromDocs.insert(new DocumentMatches(newEnt.id, docId))
      println("New entity inserted")
      return newEnt.id
    }

    def getDocumentId(fp: String): Query[Long] = {
        val d = from(EntityGraph.documents)(doc => where(doc.documentPath === fp) select (doc.id))
        return d
    }

    def insertProperty(value: String, entityId: Long) = {
        if(EntityGraph.properties.lookup(new CompositeKey2(value,entityId)).isEmpty) {
            val prop = new Property(value, entityId)
            EntityGraph.properties.insert(prop)
        }
    }

    def insertQuality(pId: Long, key: String, qual: String, strength: Int) = {
        val p = new PropertyQuality(pId, key, qual, strength)
        EntityGraph.qualitiesOfProperties.insert(p)
    }

    def insertLocation(loc: String, sId: Long) = {
        if(EntityGraph.locations.lookup(new CompositeKey2(loc, sId)).isEmpty)    {
            val l = new Location(loc, sId, None, None)
            EntityGraph.locations.insert(l)
        }
    }

    def insertLocation(loc: String, sId: Long, lat: String, lng: String) = {
        val l = new Location(loc, sId, Some(lat), Some(lng))
        EntityGraph.locations.insert(l)
    }

    def insertConnection(propId: Long, govId: Long, depId: Long, strength: Int) = {
        EntityGraph.connections.insert(new Connection(propId, govId, depId, strength))
    }



}

