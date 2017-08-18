require 'rubygems'
require 'active_support/inflector'

# This program preprocesses the blackbox.tex to insert all the schemas automatically (to save keeping
# them up to date manually).  For this to run, you need on your machine:
# - a checked out version of blackboxserver, with an up-to-date database named blackbox_development
# - access to Ruby, including the active_support gem (`sudo gem install active_support` should do it)

shown_tables = []
mentioned_event_names = []

# We fetch the list of tables direct from MySQL (assuming you have no root password set):
all_tables = `mysql -u root blackbox_development -e "show tables" | tail -n +2`.lines.map(&:chomp)

# We read the tex file from stdin:
input_contents = $stdin.readlines

# First pass to find event names in the tex file -- we need these for a substitution.
# We search all \section (and \subsection etc) titles for \lstinline!"blah"! entries: 
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

    puts "\\begin{table}[H]"
    puts "\\begin{center}"
    puts "\\caption[\\lstinline!#{table_name}! schema]{Schema for the \\lstinline!#{table_name}! table. #{caption}}"
    puts "\\label{tab:#{table_name}}" # Label the table
    puts "\\begin{tabular}{l@{\\hspace{2cm}}l@{\\hspace{1cm}}l}"
    puts "Field Name & Type & Can Be Null?\\\\ \\hline"
    # Put view columns in manually, as they are not usually available on your local machine:
    case table_name
    when "participant_identifiers_for_experiment" then
      info = [["participant_identifiers_for_experiment", "varchar(32)","NO","","NULL",""]]
    when "sessions_for_experiment"
      info = [["id","bigint(20)","NO","","0",""],
              ["user_id","bigint(20)","NO","","NULL",""],
              ["participant_id","bigint(20)","YES","","NULL",""],
              ["created_at","datetime","NO","","NULL",""],
              ["installation_details_id","bigint(20)","NO","","NULL",""],
              ["identifier","varchar(64)","NO","","NULL",""],
              ["last_sequence_id","int(11)","NO","","NULL",""],
              ["participant_identifier","varchar(32)","NO","","NULL",""]]
    else
      info = `mysql -u root blackbox_development -e "show columns from #{table_name}" | tail -n +2`.lines.map {|l| l.split "\t" }
    end
    # Make id column come first:
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
    mentioned_event_names.sort.each do |event_name|
      puts "|\"#{event_name}\"| \\dotfill & \\myref{evt:#{event_name}}\\\\"
    end
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

# We find all events in the EventName class by looking for allStrings:
all_events = []
`cat ../../src/bluej/collect/EventName.java`.lines.each do |enum_line|
  if enum_line =~ /\S+\("(\S+)\"\)/
    all_events << $1
  end
end

#Then we check that all event names mentioned in the manual match those in the source:
all_events.reject {|evt| mentioned_event_names.include? evt}.each do |event|
  result_code = 1
  $stderr.puts "Event not described: \"#{event}\""
end

mentioned_event_names.reject {|evt| all_events.include? evt}.each do |event|
  result_code = 1
  $stderr.puts "Old event described: \"#{event}\""
end


exit result_code

