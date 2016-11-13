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

import io.github.mschonaker.bundler.loader.Bundle;
import io.github.mschonaker.bundler.loader.BundleLoader;
import io.github.mschonaker.bundler.utils.Coercions;

public class Config {

	private boolean lenient = true;
	private Coercions coercions = Coercions.JRE;
	private Map<String, Bundle> bundles;

	public Config coercions(Coercions coercions) {
		this.coercions = coercions;
		return this;
	}

	public Config load(Reader reader) throws IOException {
		try {
			if (bundles == null)
				bundles = new HashMap<>();
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

	public Config lenient() {
		lenient = true;
		return this;
	}

	public Config strict() {
		lenient = false;
		return this;
	}

	Map<String, Bundle> bundles() {
		return bundles;
	}

	Coercions coercions() {
		return coercions;
	}

	boolean isLenient() {
		return lenient;
	}
}
