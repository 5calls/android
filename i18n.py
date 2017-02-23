
# Little script to make a CSV from translated strings.

# Get the english language strings
raw_strings = []
f = open("5calls/app/src/main/res/values/strings.xml", "r")
for line in f:
	line = line.strip()
	if len(line) > 0:
		raw_strings.append(line)
f.close()

# Parse them
strings = {}
for i in range(len(raw_strings)):
	line = raw_strings[i]
	if line.startswith("<!--"):
		comment = line[5 : len(line) - 4]
		i += 1
		line = raw_strings[i]
		while not line.endswith("</string>"):
			# multiline strings
			i += 1
			line += raw_strings[i]
		string_id_start = line.find("name=\"") + 6
		string_id_end = line.find("\">")
		string_id = line[string_id_start : string_id_end]
		value = line[string_id_end + 2 : len(line) - len("</string>")]
		strings[string_id] = {'comment': comment, 'value': value}

# Get the spanish language strings
raw_es_strings = []
f_es = open("5calls/app/src/main/res/values-es/strings.xml", "r")
for line in f_es:
	line = line.strip()
	if len(line) > 0:
		raw_es_strings.append(line)
f_es.close()

# Add these to the dict
for i in range(len(raw_es_strings)):
	line = raw_es_strings[i]
	if line.startswith("<string name="):
		while not line.endswith("</string>"):
			i += 1
			line += raw_es_strings[i]
		string_id_start = line.find("name=\"") + 6
		string_id_end = line.find("\">")
		string_id = line[string_id_start : string_id_end]
		value = line[string_id_end + 2 : len(line) - len("</string>")]
		strings[string_id]['value-es'] = value

# Write to a CSV
out = open("translations.csv", "w")
for string_id in strings:
	out.write("%s,\"%s\",\"%s\"" % (string_id, strings[string_id]['comment'], strings[string_id]['value']))
	if 'value-es' in strings[string_id]:
		out.write(",\"%s\"" % strings[string_id]['value-es'])
	out.write('\n')
out.close