# Bundler

Bundler is yet another library for Object/Relational mapping. It was designed to
have the simplest possible setup and usage. It's being productive in several
systems for a couple of years now.

Bundler makes extensive use of the JDBC `ResultSet` method 
`java.sql.ResultSetMetaData.getColumnClassName(int)`, which can tell at 
runtime what's the exact Java type that the `ResultSet` will return as a column
type.

## Design goal #1: Database world shall predominate over object world

What this goal means is that database world types predominate over object world
classes and, thus, the database column types define the object classes that map
them. And that's only known at runtime, not before.

> This doesn't mean that you have to guess the types of the columns until you
> run your program. The method 
> `Bundler.asJavaTypes(DataSource, PrintWriter, Character)` may help you
> find the Java types.

This goal also means that database schemas are created externally, not through
this tool, not by traversing the Java classes that you want to use to map the
database.


## Design goal #2: Simplicity, solve the problem in the correct space

The goal of this project is to help to reflect database capabilities in the
Java world, and nothing else. It's not a framework, it doesn't do caching for
instance. That's something that you will have to provide to your system with
another tool.

Bundler does not generate SQL queries, although helps with parameters in JDBC.

Bundler does not deal with method signatures, as you will see,
with the sole purpose of keeping XML files as simple and clean as possible. This
is a problem that you could rather solve with simplicity in the Java world. The
XML world is not a very nice place to solve Java method signatures identity.

## Design goal #3: Smallest footprint possible. In runtime resources, in code

You should never get a surprise from Bundler. Everything should be clean and 
smooth, and the code easy to fork and patch.

Returned objects are not proxies (although inflated objects are) not lazy, no 
weak references. The returned objects are concrete objects that you are used to
work with.

# Examples

## Hello world

Suppose that you have created a database like this:

	create table users (
		username varchar(200) not null primary key,
		password varchar(20) not null,
		realname varchar(200) not null
	);
	
	insert into users(username, password, realname) 
	values('user-a', 'secret', 'User A');
	
	insert into users(username, password, realname) 
	values('user-b', 'secret', 'User B');
	
	insert into users(username, password, realname) 
	values('user-c', 'secret', 'User C');
	
> This DDL has been written with H2 syntax. Unless said otherwise, we're using
> H2. H2 is just a choice for quick development of the examples and tests. Of
> course the tool is designed for any database.

And you want to map all of the users. You know how to select them from the
database, using SQL:

    select * from users

Also suppose that you have already defined the interface of the DAO that you
want to use:

	package io.github.mschonaker.bundler.test.daos.helloworld;
	
	import java.util.List;
	
	public interface UserDAO {
		
		void createDatabase();
		
		List<User> findAll();
	
	}
	
and:

	package io.github.mschonaker.bundler.test.daos.helloworld;

	public class User {
	
		private String username;
		private String password;
		private String realname;
	
		public String getUsername() {
			return username;
		}
	
		public void setUsername(String username) {
			this.username = username;
		}
	
		public String getPassword() {
			return password;
		}
	
		public void setPassword(String password) {
			this.password = password;
		}
	
		public String getRealname() {
			return realname;
		}
	
		public void setRealname(String realname) {
			this.realname = realname;
		}
	}

When you're familiar with JDBC DataSources and databases, you already know
that __everything else you need from this point feels like boilerplate__. This 
is the point where __we usually want the tool to make its magic and, with the 
minimum possible footprint in our code, to be the "glue" between the Object 
world and the Database world__.

So. Let's reorganize the SQL queries in an XML file matching the
names of the methods in the DAO, and with `bundler` as the root XML element, as
follows:

	<?xml version="1.0" encoding="UTF-8" ?>
	<bundler>
		<createDatabase>
			
			create table users (
				username varchar(200) not null primary key,
				password varchar(20) not null,
				realname varchar(200) not null
			);
			
			insert into users(username, password, realname) 
			values('user-a', 'secret', 'User A');
			
			insert into users(username, password, realname) 
			values('user-b', 'secret', 'User B');
			
			insert into users(username, password, realname) 
			values('user-c', 'secret', 'User C');
			
		</createDatabase>
		
		<findAll>
			select * from users
		</findAll>
	
	</bundler>

Now Bundler enters:

	UserDAO dao = Bundler.inflate(UserDAO.class);

And that's almost it. You'll need to explicitly open and close transactions. 
Like this:

	try (Transaction tx = Bundler.writeTransaction(ds)) {
			dao.createDatabase();
			tx.success();
	}

	try (Transaction tx = Bundler.readTransaction(ds)) {

		List<User> users = dao.findAll();
		// ... do something with users ...

	}
	
## Working with updates

Let's review the previous example, and let our DAO implement much more services,
like these:

	package io.github.mschonaker.bundler.test.daos.updates;

	import java.util.List;
	
	public interface UserDAO {
	
		void createSchema();
	
		List<User> findAll(int offset, int limit);
	
		User find(String string);
	
		Long count();
	
		void insert(User user);
	
		void update(User user);
	
		void upsert(User user);
	
		void delete(String username);
	}

	
The XML file will have some differences. Like parameters. Parameters always
passed as an array of objects, `params`, which can be inspected using the 
[Java Expression Language 2.2](http://download.oracle.com/otndocs/jcp/expression_language-2.2-mrel-eval-oth-JSpec/).
The alias `param` is simply an alias for the expression `params[0]`. Sounds
complex, but it's simpler when you see the full XML file:

	<?xml version="1.0" encoding="UTF-8" ?>
	<bundler>
		
		<createSchema>
			create table users (
			username varchar(200) not null primary key,
			password varchar(20) not null,
			realname varchar(200) not null
			)
		</createSchema>
		
		<findAll>
			select username, password, realname
			from users offset ${params[0]} limit ${params[1]}
		</findAll>
	
		<find>
			select username, password, realname
			from users where username = ${param}
		</find>
	
		<count>
			select count(*) from users
		</count>
	
		<upsert>
			merge into users(username, password, realname)
			values(${param.username}, ${param.password}, ${param.realname})
		</upsert>
	
		<insert>
			insert into users(username, password, realname)
			values(${param.username}, ${param.password}, ${param.realname})
		</insert>
	
		<update>
			update users set
			password = ${param.password},
			realname = ${param.realname}
			where username = ${param.username}
		</update>
	
		<delete>
			delete from users where username = ${param}
		</delete>
	</bundler>
	
## Working with `auto_increment` primary keys

Suppose that now we want to use database automatically incremented keys, a quite
useful feature from most database systems. So we re-define our `User` class to
include a numeric ID field:

	package io.github.mschonaker.bundler.test.daos.autoincrement;
	
	public class User {
	
		private Long id;
		private String username;
		private String password;
		private String realname;
	
		public Long getId() {
			return id;
		}
	
		public void setId(Long id) {
			this.id = id;
		}
	
		public String getUsername() {
			return username;
		}
	
		public void setUsername(String username) {
			this.username = username;
		}
	
		public String getPassword() {
			return password;
		}
	
		public void setPassword(String password) {
			this.password = password;
		}
	
		public String getRealname() {
			return realname;
		}
	
		public void setRealname(String realname) {
			this.realname = realname;
		}
	}
	
And add the new column in our XML file:

	<?xml version="1.0" encoding="UTF-8" ?>
	<bundler>
		<createSchema>
			create table users (
				id bigint auto_increment not null primary key,
				username varchar(200) not null,
				password varchar(20) not null,
				realname varchar(200) not null,
				unique key (username)
			)
		</createSchema>
	
		<findAll>
			select id, username, password, realname
			from users
		</findAll>
	
		<find>
			select id, username, password, realname
			from users where id = ${param}
		</find>
	
		<insert>
			insert into users(username, password, realname)
			values(${param.username}, ${param.password}, ${param.realname})
		</insert>
	</bundler>	
	
And now we redefine the DAO as follows, with the subtle difference that now our
`insert` method returns a `Long`.

	package io.github.mschonaker.bundler.test.daos.autoincrement;
	
	import java.util.List;
	
	public interface UserDAO {
	
		void createSchema();
	
		List<User> findAll();
	
		User find(Long id);
	
		Long insert(User user);
	
	}

We don't need further modifications, Bundler knows ('cause the JDBC can tell) 
that the query was an update, and also knows that you want a `Long` as the 
result of your update query. Thus, it will automatically return the generated
keys from the query. 

That becomes very handy and you could insert an already filled-in user like 
this:

	try (Transaction tx = Bundler.writeTransaction(ds)) {
		Long id = dao.insert(user);
		user.setId(id);
		tx.success();
	}	

or directly: 

	try (Transaction tx = Bundler.writeTransaction(ds)) {
		user.setId(dao.insert(user))	
		tx.success();
	}

