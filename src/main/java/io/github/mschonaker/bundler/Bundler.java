package io.github.mschonaker.bundler;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.github.mschonaker.bundler.loader.Bundle;
import io.github.mschonaker.bundler.utils.Beans;
import io.github.mschonaker.bundler.utils.Methods;

/**
 * A small API for Object-Relational mapping.
 *
 * @author mschonaker
 */
public class Bundler {

	/**
	 * Inflates the interface.
	 */
	public static <T> T inflate(Class<T> type) throws IOException {
		return inflate(type, null);
	}

	/**
	 * Inflates the interface.
	 */
	public static <T> T inflate(Class<T> type, Config config) throws IOException {

		Objects.requireNonNull(type, "type is required");

		if (config == null)
			config = new Config();

		Bundle root = new Bundle();
		Map<String, Bundle> bundles = config.bundles();

		if (bundles == null) {
			config.loadResource(type);
			bundles = config.bundles();
		}

		root.children = bundles;

		if (!config.isLenient())
			validate(root, type);

		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
				new BundlerInvocationHandler(root, config)));
	}

	private static void validate(Bundle bundle, Class<?> type) {
		Set<String> methods = Arrays.stream(type.getMethods()).map(Method::getName).collect(Collectors.toSet());

		// The names of the queries in the file.
		Set<String> queries = bundle.children != null ? bundle.children.keySet() : Collections.<String>emptySet();

		Set<String> temp = new HashSet<String>();
		temp.addAll(methods);
		temp.removeAll(queries);

		if (temp.size() > 0)
			throw new BundlerValidationException("Couldn't map instance of class " + type
					+ ". Methods not found in queries file: " + temp.toString());

		temp.clear();
		temp.addAll(queries);
		temp.removeAll(methods);

		if (temp.size() > 0)
			throw new BundlerValidationException(
					"Couldn't map instance of class " + type + ". No method found for queries: " + temp.toString());
	}

	// ---------------------------------------------------------------------
	// Proxy methods.

	private static class BundlerInvocationHandler implements InvocationHandler {

		private final Bundle bundle;
		private final Config config;

		public BundlerInvocationHandler(Bundle bundle, Config config) {
			this.bundle = bundle;
			this.config = config;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			Bundle localBundle = bundle.children.get(method.getName());

			if (localBundle == null)
				throw new IllegalStateException("Bundle not for method: " + method);

			try {

				CurrentTransaction tx = locateTransaction();

				ParamContext params = new ParamContext();
				params.set("params", args);
				if (args != null && args.length > 0)
					params.set("param", args[0]);

				return execute(tx, localBundle, config, method, params);

			} catch (Throwable t) {

				if (Methods.doesThrow(method, t))
					throw t;

				throw new BundlerSQLException(t);

			}
		}
	}

	// ---------------------------------------------------------------------
	// Database.

	private static Object execute(CurrentTransaction transaction, Bundle bundle, Config config, Method method,
			ParamContext params) throws Exception {

		// Special case: no root sql.
		if (bundle.sql == null) {

			if (bundle.children == null)
				return null;

			if (Methods.returnsList(method) || Methods.returnsPrimitive(method))
				throw new IllegalArgumentException();

			Object object = method.getReturnType().newInstance();

			for (Bundle sub : bundle.children.values()) {

				Object value = execute(transaction, sub, config,
						new PropertyDescriptor(sub.name, method.getReturnType()).getReadMethod(), params);

				Beans.setNestedProperty(object, sub.name, value, config.isLenient(), config.coercions());
			}

			return object;
		}

		boolean isReturning = !Methods.returnsVoid(method);

		try (PreparedStatement ps = transaction.connection.prepareStatement(bundle.sql, //
				isReturning ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {

			// Parameters.
			if (bundle.expressions != null) {
				int i = 1;
				for (String parameterExpression : bundle.expressions) {
					ps.setObject(i, params.get(parameterExpression));
					i++;
				}
			}

			// Execution.
			boolean isQuery = ps.execute();

			// Results.
			if (!isReturning)
				return null;

			try (ResultSet rs = isQuery ? ps.getResultSet() : ps.getGeneratedKeys()) {

				Result result = new Result(rs, config);

				Result.OnEach onEach = new Result.OnEach() {

					@Override
					public <T> T onEach(Class<T> type, T object) throws Exception {

						if (bundle.children != null)
							for (Bundle sub : bundle.children.values()) {

								params.set("parent", object);

								Object value = execute(transaction, sub, config,
										new PropertyDescriptor(sub.name, type).getReadMethod(), params);

								Beans.setNestedProperty(object, sub.name, value, config.isLenient(),
										config.coercions());
							}

						return object;
					}

				};

				return result.toReturnTypeOf(method, onEach);
			}
		}
	}

	// ---------------------------------------------------------------------
	// Transactions.
	//
	// A connection per thread per DataSource is allowed. The reference to the
	// thread is implicit in the ThreadLocal. Thus, there's a map
	// IdentityMap) per DataSource indicating the connection associated, if any.
	//
	// Additionally, along with a connection, there is a boolean variable
	// with "success" marks. So in the end of the transaction the data is
	// committed or rolled back.
	//
	// The methods accept a DataSource or an inflated object.

	private static final ThreadLocal<CurrentTransaction> current = new InheritableThreadLocal<CurrentTransaction>();

	public static interface Transaction extends AutoCloseable {

		void success();

		@Override
		public void close();

	}

	public static Transaction readTransaction(DataSource ds) {
		return connect(ds, true, false);
	}

	public static Transaction inheritReadTransaction(DataSource ds) {
		return connect(ds, true, true);
	}

	public static Transaction writeTransaction(DataSource ds) {
		return connect(ds, false, false);
	}

	public static Transaction inheritWriteTransaction(DataSource ds) {
		return connect(ds, false, true);
	}

	private static Transaction connect(DataSource ds, boolean readOnly, boolean inherit) {

		if (ds == null)
			throw new IllegalArgumentException("DataSource");

		try {

			CurrentTransaction oldTx = current.get();
			if (oldTx == null || !inherit) {
				CurrentTransaction newTx = new CurrentTransaction();
				newTx.connection = ds.getConnection();
				newTx.connection.setReadOnly(readOnly);
				newTx.connection.setAutoCommit(false);
				newTx.previous = oldTx;
				current.set(newTx);
				return newTx;
			}

			return oldTx;

		} catch (SQLException e) {
			throw new BundlerSQLException(e);
		}
	}

	private static class CurrentTransaction implements Transaction {

		Connection connection;
		boolean success = false;
		public CurrentTransaction previous;

		@Override
		public void success() {
			success = true;
		}

		@Override
		public void close() {
			try {

				if (success)
					connection.commit();
				else
					connection.rollback();

			} catch (SQLException e) {
				throw new BundlerSQLException(e);
			} finally {
				try {

					connection.close();

				} catch (SQLException e) {
					throw new BundlerSQLException(e);
				} finally {

					current.set(previous);

				}
			}
		}
	}

	public static void assertConnected() {
		locateTransaction();
	}

	private static CurrentTransaction locateTransaction() {
		CurrentTransaction tx = current.get();

		if (tx == null)
			throw new IllegalStateException("Not connected");

		return tx;
	}

	// -------------------------------------------------------------------------
	// Dump.

	/**
	 * Helper method that dumps all the tables in the database, including the
	 * Java types that should be used in mapping.
	 */
	public static void dumpDB(DataSource ds, PrintWriter writer, Character identifierQuote) {

		try (Connection connection = ds.getConnection()) {

			DatabaseMetaData md = connection.getMetaData();

			try (ResultSet tables = md.getTables(null, null, null, new String[] { "TABLE" })) {

				while (tables.next()) {

					String tableName = tables.getString("TABLE_NAME");
					writer.println(tableName);
					writer.println("------------------------------------");

					StringBuilder sql = new StringBuilder();
					sql.append("select * from ");
					if (identifierQuote != null)
						sql.append(identifierQuote);
					sql.append(tableName);
					if (identifierQuote != null)
						sql.append(identifierQuote);
					sql.append(" where false");

					try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
						try (ResultSet rs = ps.executeQuery()) {

							ResultSetMetaData qmd = rs.getMetaData();

							for (int i = 1; i <= qmd.getColumnCount(); i++) {
								writer.print(qmd.getColumnName(i));
								writer.print(": ");
								writer.print(qmd.getColumnClassName(i));
								writer.println();
							}

							writer.println();
						}
					}

					writer.flush();
				}
			}
		} catch (SQLException e) {
			throw new BundlerSQLException(e);
		}
	}
}
