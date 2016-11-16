package org.fylia.jappa.test.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class SqlUtil {
   /**
	 * Gets all statements from an SQL file. Removes all singleline -- and // comments and multiline /* ... * / comments.
	 * Ignores comment statements in strings, escaped single quotes (by using double singlequotes).
	 * Statement separator is ';'
	 * 
	 * @param sqlFile the sqlFile to split into statements
	 * @return the sqlstatements
	 */
	public static String[] getStatements(final String sqlFile) {
		try {
			StringReader reader = new StringReader(sqlFile);
			return getStatements(reader);
		} catch(IOException e) {
			// Should not happen
			throw new RuntimeException(e);
		}
	}

   /**
	 * Gets all statements from an SQL file. Removes all singleline -- and // comments and multiline /* ... * / comments.
	 * Ignores comment statements in strings, escaped single quotes (by using double singlequotes).
	 * Statement separator is ';'
	 * 
	 * @param sqlFile the sqlFile to split into statements
	 * @return the sqlstatements
	 * @throws IOException 
	 */
	public static String[] getStatements(final Reader sqlFile) throws IOException {
		List<String> statements = new ArrayList<String>();
		boolean inMlComment=false;
		boolean inSlComment=false;
		boolean inString=false;
		StringBuilder statement = new StringBuilder();
		int next = Integer.MIN_VALUE;
		while (next!=-1) {
			int current = next;
			next = sqlFile.read();
			if (current == Integer.MIN_VALUE) {
				continue;
			}
			if (inString) {
				statement.append((char)current);
				if (current=='\'' && next=='\'') {
					statement.append('\'');
					next = sqlFile.read();
				} else if (current=='\'') {
					inString = false;
				}
			} else if (inMlComment) {
				if (current=='*' && next=='/') {
					inMlComment = false;
					next = sqlFile.read();
				}
			} else if (inSlComment) {
				if (current=='\n' || current=='\r') {
					inSlComment = false;
				}
			} else {
				switch(current) {
				case '/':
					if (next=='*') {
						inMlComment = true;
						next = sqlFile.read();
					} else if (next=='/') {
						inSlComment = true;
						next = sqlFile.read();
					}
					break;
				case '-':
					if (next=='-') {
						inSlComment = true;
						next = sqlFile.read();
					} else {
						statement.append((char)current);
					}
					break;
				case '\'':
					inString = true;
					statement.append('\'');
					break;
				case ';':
					// End of statement
					String finalStatement = StringUtils.trimToNull(statement.toString());
					statement.setLength(0);
					if (finalStatement!=null) {
						statements.add(finalStatement);
					}
					break;
				default:
					statement.append((char)current);
				}
			}
		}

		String finalStatement = StringUtils.trimToNull(statement.toString());
		if (finalStatement!=null) {
			statements.add(finalStatement);
		}
        
		return statements.toArray(new String[statements.size()]);
	}
}
