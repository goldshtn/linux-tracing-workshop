#include <fstream>
#include <iostream>
#include <iterator>
#include <regex>
#include <vector>

#include <omp.h>

class pargrep {
    std::string dir_;
    std::regex regexp_;
    std::vector<std::string> results_;
public:
    pargrep(std::string dir, std::string regexp)
        : dir_(std::move(dir)), regexp_(regexp) {
    }

    void run() {
        std::vector<std::string> files {
            // TODO use actual files from the provided directory
            "pargrep.cc",
            "pargrep.cc",
            "pargrep.cc",
            "pargrep.cc"
        };
#pragma omp parallel for
        for (int i = 0; i < files.size(); ++i) {
            do_one_file(files[i]);
        }
        std::copy(results_.begin(), results_.end(),
                  std::ostream_iterator<std::string>(std::cout, "\n"));
    }
private:
    void do_one_file(std::string const& filename) {
        std::cout << omp_get_thread_num() << " " << filename << "\n";
        std::string line;
        std::ifstream ifs(filename.c_str());
        while (std::getline(ifs, line)) {
            if (std::regex_search(line, regexp_)) {
                results_.push_back(filename + ": " + line);
            }
        }
    }
};

void usage() {
    std::cerr << "USAGE: pargrep <directory> <regexp>\n";
    exit(1);
}

int main(int argc, char* argv[]) {
    if (argc < 3) {
        usage();
    }

    omp_set_num_threads(4);
    pargrep grepper(argv[1], argv[2]);
    grepper.run();

    return 0;
}
