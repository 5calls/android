
# Little script to make a CSV from translated strings.

from collections import OrderedDict

# Get the english language strings
raw_strings = []
f = open("5calls/app/src/main/res/values/strings.xml", "r")
for line in f:
	line = line.strip()
	if len(line) > 0:
		raw_strings.append(line)
f.close()

# Parse them
strings = OrderedDict()
current_comment = ""
i = 0
while i < len(raw_strings):
	line = raw_strings[i]
	if line.startswith("<!--"):
		# Update the current comment
		current_comment = line[5 : len(line) - 4]
	elif line.startswith("<string name="):
		# Process the string, using the current comment
		full_line = line
		while not full_line.endswith("</string>"):
			# multiline strings
			i += 1
			full_line += raw_strings[i]
		string_id_start = full_line.find("name=\"") + 6
		string_id_end = full_line.find("\">")
		string_id = full_line[string_id_start : string_id_end]
		value = full_line[string_id_end + 2 : len(full_line) - len("</string>")]
		strings[string_id] = {'comment': current_comment, 'value': value}
	i += 1

# Get the spanish language strings
raw_es_strings = []
f_es = open("5calls/app/src/main/res/values-es/strings.xml", "r")
for line in f_es:
	line = line.strip()
	if len(line) > 0:
		raw_es_strings.append(line)
f_es.close()

# Add these to the dict
i = 0
while i < len(raw_es_strings):
	line = raw_es_strings[i]
	if line.startswith("<string name="):
		full_line = line
		while not full_line.endswith("</string>"):
			i += 1
			full_line += raw_es_strings[i]
		string_id_start = full_line.find("name=\"") + 6
		string_id_end = full_line.find("\">")
		string_id = full_line[string_id_start : string_id_end]
		value = full_line[string_id_end + 2 : len(full_line) - len("</string>")]
		# Only add Spanish translation if the string exists in English
		if string_id in strings:
			strings[string_id]['value-es'] = value
	i += 1

# Write to a CSV
out = open("translations.csv", "w")
for string_id in strings:
	out.write("%s,\"%s\",\"%s\"" % (string_id, strings[string_id]['comment'], strings[string_id]['value']))
	if 'value-es' in strings[string_id]:
		out.write(",\"%s\"" % strings[string_id]['value-es'])
	out.write('\n')
out.close