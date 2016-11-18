package io.github.mschonaker.bundler.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

import io.github.mschonaker.bundler.Bundler;
import io.github.mschonaker.bundler.Config;

public abstract class AbstractTest {

	protected static DataSource ds;

	protected static <T> T initialize(Class<T> type) throws IOException {
		return initialize(type, null);
	}

	protected static <T> T initialize(Class<T> type, Config config) throws IOException {
		JdbcDataSource dataSource = new JdbcDataSource();

		dataSource.setURL("jdbc:h2:mem:sampledb" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
		dataSource.setUser("sa");
		dataSource.setPassword("");

		ds = dataSource;
		Bundler.asJavaTypes(dataSource, new PrintWriter(System.out), null);

		return Bundler.inflate(type, config);
	}
}
