/*
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 */
package com.sap.poc.jgit.storage.jdbc;

import static com.sap.poc.jgit.storage.jdbc.JdbcEnDecoder.decodeRowKey;
import static com.sap.poc.jgit.storage.jdbc.JdbcEnDecoder.encodeRowKey;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jgit.storage.dht.DhtException;
import org.eclipse.jgit.storage.dht.RepositoryKey;
import org.eclipse.jgit.storage.dht.RepositoryName;
import org.eclipse.jgit.storage.dht.spi.RepositoryIndexTable;

public class JdbcRepositoryIndexTable extends JdbcSqlHelper implements
		RepositoryIndexTable {
	private JdbcDatabase db;

	public JdbcRepositoryIndexTable(final JdbcDatabase db) {
		this.db = db;
	}

	@Override
	public RepositoryKey get(final RepositoryName name) throws DhtException,
			TimeoutException {
		Connection conn = null;

		try {
			if (name != null) {
				conn = db.getConnection();
				final PreparedStatement stmt = conn
						.prepareStatement(SELECT_REPO_KEY_FROM_REPO_IDX);
				stmt.setString(1, name.asString());
				stmt.execute();
				final ResultSet resSet = stmt.getResultSet();
				if (resSet != null && resSet.next()) {
					// Exists
					final String sRepoKey = resSet.getString(1);
					if (sRepoKey != null && sRepoKey.length() > 0)
						return RepositoryKey.fromBytes(decodeRowKey(sRepoKey));
				}
				return null;
			}
			throw new DhtException("Invalid parameter"); // TODO externalize
		} catch (SQLException e) {
			throw new DhtException(e);
		} finally {
			closeConnection(conn);
		}
	}

	@Override
	public void putUnique(final RepositoryName name, final RepositoryKey key)
			throws DhtException, TimeoutException {
		Connection conn = null;

		try {
			if (name != null && key != null) {
				final String sRepoName = name.asString();
				conn = db.getConnection();
				PreparedStatement stmt = conn
						.prepareStatement(SELECT_EXISTS_FROM_REPO_IDX);
				stmt.setString(1, sRepoName);
				stmt.execute();
				final ResultSet resSet = stmt.getResultSet();
				if (resSet != null && resSet.next())
					// Exists
					throw new DhtException("Repository name already exists"); // TODO
																				// externalize
				stmt = conn.prepareStatement(INSERT_INTO_REPO_IDX);
				stmt.setString(1, encodeRowKey(key));
				stmt.setString(2, sRepoName);
				stmt.executeUpdate();
				// TODO check result
				return;
			}
			throw new DhtException("Invalid parameters"); // TODO externalize
		} catch (UnsupportedEncodingException e) {
			throw new DhtException(e);
		} catch (SQLException e) {
			throw new DhtException(e);
		} finally {
			closeConnection(conn);
		}
	}
}
