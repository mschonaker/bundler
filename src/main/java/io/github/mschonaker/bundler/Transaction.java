package io.github.mschonaker.bundler;

public interface Transaction extends AutoCloseable {

	void success();

	@Override
	public void close();

}
