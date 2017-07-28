blackbox.pdf: blackbox.tex inject_schema.rb
	ruby inject_schema.rb < blackbox.tex > out.tex
	pdflatex out.tex && pdflatex out.tex && pdflatex out.tex
	mv out.pdf blackbox.pdf

clean:
	rm -f *.aux *.log *.out blackbox.pdf out.pdf

rebuild: clean blackbox.pdf
