while gets
  line = $_
  if line =~ /^%schema:(\S+)/
    puts "\\begin{tabular}{lll}"
    `mysql -u root blackbox_development -e "show columns from #{$1}"`.each_line do |l|
      cols = l.split "\t"
      puts "|#{cols[0]}| & |#{cols[1]}| & |#{cols[2]}| \\\\"
    end
    puts "\\end{tabular}"
  else
    puts line
  end
end
