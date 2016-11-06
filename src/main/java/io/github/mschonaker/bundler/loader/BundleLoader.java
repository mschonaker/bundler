package io.github.mschonaker.bundler.loader;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BundleLoader {

	private static class BundleHandler extends DefaultHandler {

		private final StringBuilder text = new StringBuilder();
		private final Stack<Bundle> stack = new Stack<Bundle>();
		private final Map<String, Bundle> bundles;

		public BundleHandler(Map<String, Bundle> bundles) {
			this.bundles = bundles;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			text.append(ch, start, length);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

			Bundle bundle = new Bundle();
			bundle.name = qName;

			if (stack.isEmpty()) {

				if (!"bundler".equals(qName))
					throw new SAXException("Expected \"bundler\" element, got \"" + qName + "\"");

				bundle.children = bundles;

			} else {

				Bundle parent = stack.peek();
				flushText(parent);
				if (parent.children == null)
					parent.children = new LinkedHashMap<String, Bundle>();
				parent.children.put(bundle.name, bundle);
			}

			stack.push(bundle);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {

			if (stack.size() > 1) {
				Bundle bundle = stack.pop();
				flushText(bundle);
				extractExpressions(bundle);
			}

			text.delete(0, text.length());
		}

		private void flushText(Bundle bundle) {

			String t = text.toString().trim();

			if (!t.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				if (bundle.sql != null)
					sb.append(bundle.sql);
				sb.append(text.toString());
				bundle.sql = sb.toString();
			}

			text.delete(0, text.length());
		}
	}

	private static final Pattern EXPRESSION_PATTERN = Pattern.compile("(\\$\\{([^\\}]*)\\})");

	private static void extractExpressions(Bundle bundle) {

		if (bundle.sql == null)
			return;

		List<String> expressions = new LinkedList<String>();
		StringBuilder sb = new StringBuilder();

		int replacedStart = 0;

		Matcher matcher = EXPRESSION_PATTERN.matcher(bundle.sql);
		while (matcher.find()) {

			int start = matcher.start();
			int end = matcher.end();

			sb.append(bundle.sql.substring(replacedStart, start));
			sb.append('?');
			replacedStart = end;

			expressions.add(matcher.group(2));
		}

		sb.append(bundle.sql.subSequence(replacedStart, bundle.sql.length()));

		String sql = sb.toString().trim();

		if (!sql.isEmpty())
			bundle.sql = sql;

		if (!expressions.isEmpty())
			bundle.expressions = expressions;
	}

	public static void load(Map<String, Bundle> bundles, Reader reader) throws Exception {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();

		BundleHandler handler = new BundleHandler(bundles);

		parser.parse(new InputSource(reader), handler);
	}
}
