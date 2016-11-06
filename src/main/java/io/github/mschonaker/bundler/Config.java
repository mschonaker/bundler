package io.github.mschonaker.bundler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import io.github.mschonaker.bundler.coercions.Coercions;
import io.github.mschonaker.bundler.loader.Bundle;
import io.github.mschonaker.bundler.loader.BundleLoader;

public class Config {

	private Coercions coercions = Coercions.JRE;
	private Map<String, Bundle> bundles = new HashMap<>();

	public Config coercions(Coercions coercions) {
		this.coercions = coercions;
		return this;
	}

	Coercions coercions() {
		return coercions;
	}

	public Config load(Reader reader) throws IOException {
		try {
			BundleLoader.load(bundles, new BufferedReader(reader));
		} catch (Exception e) {
			throw new IOException(e);
		}
		return this;
	}

	public Config load(InputStream in) throws IOException {
		load(new InputStreamReader(in));
		return this;
	}

	public Config load(InputStream in, Charset cs) throws IOException {
		load(new InputStreamReader(in, cs));
		return this;
	}

	public Config load(File file) throws IOException {
		load(new FileInputStream(file));
		return this;
	}

	public Config load(Path path) throws IOException {
		load(new FileInputStream(path.toFile()));
		return this;
	}

	public Config loadResource(Class<?> type) throws IOException {
		String name = type.getName().replace('.', '/') + "-bundler.xml";
		InputStream resource = type.getClassLoader().getResourceAsStream(name);
		if (resource == null)
			throw new FileNotFoundException("Unble to locate resource " + name);
		load(resource);
		return this;
	}

	public Config loadResource(String name) throws IOException {
		load(Config.class.getClassLoader().getResourceAsStream(name));
		return this;
	}

	Map<String, Bundle> bundles() {
		return bundles;
	}

}
