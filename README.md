* Atomize
A command-line tool for managing Atom feeds written in Babashka.

** Description
Atomize is a simple utility that helps you manage Atom feed files. It allows you to add new entries to an existing Atom feed while maintaining a specified limit on the number of entries. If no feed file exists, it will create one with a default template.

** Installation

=bbin install io.github.200ok-ch/atomize=

** Usage
#+begin_src shell
atomize [-i [-b]] [-n=<entry-limit>] <atom-file>
#+end_src

** Options
- =-i, --inplace=: Update the atom file in place
- =-b, --backup=: Keep backup of original atom file (only works with -i)
- =-n, --entry-limit=<entry-limit>=: Number of entries to keep [default: 20]
- =-h, --help=: Show help message

** Input Format
The script expects a JSON input through stdin with the following structure:
#+begin_src json
{
  "title": "Entry Title",
  "url": "https://example.com/entry",
  "id": "optional-unique-id",
  "updated": "optional-timestamp",
  "summary": "Entry summary",
  "content": "Entry content in HTML",
  "author": "Author name",
  "email": "author@example.com"
}
#+end_src

All fields are optional and will use default values if not provided:
- title: "Untitled"
- id: Random UUID
- updated: Current timestamp
- summary: "(summary missing)"
- content: "(content missing)"
- author: System username or "(author missing)"
- email: "(email missing)"

** Example Usage
1. Create a new feed and add an entry:
   #+begin_src shell
   echo '{"title": "My First Post", "content": "Hello World!"}' | atomize.bb -i feed.atom
   #+end_src

2. Update existing feed with backup:
   #+begin_src shell
   echo '{"title": "Another Post"}' | atomize.bb -i -b -n=10 feed.atom
   #+end_src

** License
tbd.
