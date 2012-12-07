shown_tables = []

while gets
  line = $_
  if line =~ /^%schema:(\S+)/
    table_name = $1
    shown_tables << table_name
    puts "\\label{tab:#{table_name}}" # Label the section, not the figure
    puts "\\begin{figure}[h!]"
    puts "\\begin{center}"
    puts "\\begin{tabular}{l@{\\hspace{2cm}}l@{\\hspace{1cm}}l}"
    puts "Field Name & Type & Can Be Null?\\\\ \\hline"
    `mysql -u root blackbox_development -e "show columns from #{table_name}" | tail -n +2`.each_line do |l|
      cols = l.split "\t"
      puts "|#{cols[0]}| & |#{cols[1]}| & |#{cols[2]}| \\\\"
    end
    puts "\\end{tabular}"
    puts "\\caption{Schema for \\lstinline|#{table_name}|}"
    puts "\\end{center}"
    puts "\\end{figure}"
  else
    puts line
  end
end
