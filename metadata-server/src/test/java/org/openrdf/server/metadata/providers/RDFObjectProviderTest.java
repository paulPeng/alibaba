package org.openrdf.server.metadata.providers;

import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class RDFObjectProviderTest extends MetadataServerTestCase {

	@rdf("urn:test:Document")
	public interface Document {
		@operation("author")
		@rdf("urn:test:author")
		Person getAuthor();
		@operation("author")
		void setAuthor(Person author);
		@operation("contributors")
		@rdf("urn:test:contributor")
		Set<Person> getContributors();
		@operation("contributors")
		void setContributors(Set<Person> contributors);
	}

	@rdf("urn:test:Person")
	public interface Person {
		@rdf("urn:test:name")
		String getName();
		void setName(String name);
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Document.class);
		config.addConcept(Person.class);
		super.setUp();
	}

	public void testNamedAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person author = con.addType(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class).setAuthor(author);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAnonyoumsAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person author = con.addType(con.getObjectFactory().createObject(), Person.class);
		author.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class).setAuthor(author);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAddingNamedAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person author = con.addType(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
		web.header("Content-Location", base+"/auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testRemovingNamedAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person author = con.addType(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class).setAuthor(author);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		web.delete();
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testAddingRelativeAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person author = con.addType(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
		web.header("Content-Location", "auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAddingAnonyoumsAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		con.addType(con.getObject(base+"/doc"), Document.class);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
		Model model = new LinkedHashModel();
		BNode auth = vf.createBNode();
		model.add(auth, RDF.TYPE, vf.createURI("urn:test:Person"));
		model.add(auth, vf.createURI("urn:test:name"), vf.createLiteral("James"));
		web.type("application/rdf+xml").put(model);
		model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testNamedContributors() throws Exception {
		ObjectConnection con = repository.getConnection();
		Document document = con.addType(con.getObject(base+"/doc"), Document.class);
		Person contributor = con.addType(con.getObject(base+"/james"), Person.class);
		contributor.setName("James");
		document.getContributors().add(contributor);
		contributor = con.addType(con.getObject(base+"/megan"), Person.class);
		contributor.setName("Megan");
		document.getContributors().add(contributor);
		con.close();
		WebResource web = client.path("/doc").queryParam("contributors", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("Megan")));
	}

	public void testAnonyoumsContributors() throws Exception {
		ObjectConnection con = repository.getConnection();
		Document document = con.addType(con.getObject(base+"/doc"), Document.class);
		Person contributor = con.addType(con.getObjectFactory().createObject(), Person.class);
		contributor.setName("James");
		document.getContributors().add(contributor);
		contributor = con.addType(con.getObjectFactory().createObject(), Person.class);
		contributor.setName("Megan");
		document.getContributors().add(contributor);
		con.close();
		WebResource web = client.path("/doc").queryParam("contributors", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("Megan")));
	}

	public void testAddingNamedContributor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person contributors = con.addType(con.getObject(base+"/auth"), Person.class);
		contributors.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class);
		con.close();
		WebResource web = client.path("/doc").queryParam("contributors", "");
		web.header("Content-Location", base+"/auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testRemovingNamedContributors() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person contributor = con.addType(con.getObject(base+"/auth"), Person.class);
		contributor.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class).getContributors().add(contributor);
		con.close();
		WebResource web = client.path("/doc").queryParam("contributors", "");
		web.delete();
		assertTrue(web.accept("application/rdf+xml").get(Model.class).isEmpty());
	}

	public void testAddingRelativeContributor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person contributors = con.addType(con.getObject(base+"/auth"), Person.class);
		contributors.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class);
		con.close();
		WebResource web = client.path("/doc").queryParam("contributors", "");
		web.header("Content-Location", "auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAddingAnonyoumsContributors() throws Exception {
		ObjectConnection con = repository.getConnection();
		con.addType(con.getObject(base+"/doc"), Document.class);
		con.close();
		WebResource web = client.path("/doc").queryParam("contributors", "");
		Model model = new LinkedHashModel();
		BNode auth = vf.createBNode();
		model.add(auth, RDF.TYPE, vf.createURI("urn:test:Person"));
		model.add(auth, vf.createURI("urn:test:name"), vf.createLiteral("James"));
		auth = vf.createBNode();
		model.add(auth, RDF.TYPE, vf.createURI("urn:test:Person"));
		model.add(auth, vf.createURI("urn:test:name"), vf.createLiteral("Megan"));
		web.type("application/rdf+xml").put(model);
		model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("Megan")));
	}
}
