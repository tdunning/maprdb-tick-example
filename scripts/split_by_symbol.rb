#!/usr/bin/env ruby

cores = 12
symbols = []
File.open("src/test/resources/symbols_only.tsv").each_line do |line|
	symbols.push(line.strip)
end

symbols = symbols.sort
step = symbols.length / cores
(0..symbols.length).step(step).each do |i|
	symbols_in_this_iter = []
	(i..i+step-1).each do |k|
		symbols_in_this_iter.push("'#{symbols[k]}'") if symbols[k]
	end
	s = "( #{symbols_in_this_iter.join(",")} )"
	print "create table s20150518_#{symbols[i]} as (select t.symbol.Symbol as symbol, t.`timestamp`, t.`open` as `open`,t.high as high, t.low as low, t.`close` as `close`, t.volume as volume from `eoddata/2015-05-18/` t where t.symbol.Symbol in #{s} order by `timestamp` asc);\n"
end

