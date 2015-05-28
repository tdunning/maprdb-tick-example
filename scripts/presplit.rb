#!/usr/bin/env ruby

cores = 12
symbols = []
File.open("symbols_only.tsv").each_line do |line|
	symbols.push(line.strip)
end

symbols = symbols.sort
step = symbols.length / cores
print "["
(0..symbols.length).step(step).each do |i|
	print "'#{symbols[i]}',"
end
print "]"
