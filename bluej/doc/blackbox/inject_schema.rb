require 'rubygems'
require 'active_support/inflector'

shown_tables = []
mentioned_event_names = []

all_tables = `mysql -u root blackbox_development -e "show tables" | tail -n +2`.lines.map(&:chomp)

input_contents = $stdin.readlines

#First pass to find event names -- we need these for a substitution:
input_contents.each do |line|
  if line =~ /^\\\S*section\{/
    line.scan(/\\lstinline\!"([^!"]+)"\!/).each do |groups|
      mentioned_event_names << groups[0]
    end
  end
end

#Second pass to process file:
input_contents.each do |line|
  if line =~ /^%schema:(\S+)(\s.*)?/
    table_name = $1
    caption = $2
    shown_tables << table_name
    puts "\\label{tab:#{table_name}}" # Label the section, not the figure
    puts "\\begin{table}[H]"
    puts "\\begin{center}"
    puts "\\caption[\\lstinline!#{table_name}! schema]{Schema for the \\lstinline!#{table_name}! table. #{caption}}"
    puts "\\begin{tabular}{l@{\\hspace{2cm}}l@{\\hspace{1cm}}l}"
    puts "Field Name & Type & Can Be Null?\\\\ \\hline"
    info = `mysql -u root blackbox_development -e "show columns from #{table_name}" | tail -n +2`.lines.map {|l| l.split "\t" }
    info.sort_by {|c| c[0] == "id" ? "" : c[0]}.each do |cols|
      t = cols[1]
      #Remove display width for int, tinyint, bigint:
      if t =~ /^(.*int)\(\d+\)/
        t = $1
      end
      f = cols[0]
      if f =~ /^(.*)_id$/ && all_tables.include?(ActiveSupport::Inflector.pluralize($1))
        
        f = "\\hyperref[tab:#{ActiveSupport::Inflector.pluralize($1)}]{\\lstinline!#{f}!}"
      else
        f = "|#{f}|"
      end
      puts "#{f} & |#{t}| & |#{cols[2]}| \\\\"
    end
    puts "\\end{tabular}"
    puts "\\end{center}"
    puts "\\end{table}"
  elsif line =~ /^%hidden:(\S+)/
    shown_tables << $1
    puts "\\label{tab:#{$1}}"
  elsif line =~ /^%table:event_names/
    puts "\\begin{tabular}{p{6cm}@{}l}"
    mentioned_event_names.sort.each do |event_name|
      puts "|\"#{event_name}\"| \\dotfill & \\myref{evt:#{event_name}}\\\\"
    end
    puts "\\end{tabular}"
  elsif line =~ /^\\\S*section\{/
    puts line
    line.scan(/\\lstinline\!"([^!"]+)"\!/).each do |groups|
      puts "\\label{evt:#{groups[0]}}"
    end
  else
    puts line
  end

end

# Treat Rails' internal tables as shown:
shown_tables << "schema_migrations"

result_code = 0

all_tables.reject {|tab| shown_tables.include? tab}.each do |table|
  result_code = 1
  $stderr.puts "Table not described: \"#{table}\""
end

#Could also make sure all fields are documented...

all_events = []
`cat ../../src/bluej/collect/EventName.java`.lines.each do |enum_line|
  if enum_line =~ /\S+\("(\S+)\"\)/
    all_events << $1
  end
end

all_events.reject {|evt| mentioned_event_names.include? evt}.each do |event|
  result_code = 1
  $stderr.puts "Event not described: \"#{event}\""
end

mentioned_event_names.reject {|evt| all_events.include? evt}.each do |event|
  result_code = 1
  $stderr.puts "Old event described: \"#{event}\""
end


exit result_code

