#include <iostream>
#include <fstream>
#include <algorithm>
#include <string>
#include <map>
#include <vector>
#include <iterator>
#include <memory>

class word_counter;

class input_reader
{
private:
        std::shared_ptr<word_counter> counter_;
public:
        void set_counter(std::shared_ptr<word_counter> counter);
        std::string next_filename();
};

class word_counter
{
private:
        std::vector<std::string> words_;
        bool done_ = false;
        std::shared_ptr<input_reader> reader_;
public:
        word_counter(std::shared_ptr<input_reader> reader);
        bool done() const;
        std::map<std::string, int> word_count();
};

void input_reader::set_counter(std::shared_ptr<word_counter> counter)
{
        counter_ = counter;
}

std::string input_reader::next_filename()
{
        std::string input;
        std::cout << "filename or 'quit'> ";
        std::getline(std::cin, input);
        if (input == "quit")
                return "";
        return input;
}

word_counter::word_counter(std::shared_ptr<input_reader> reader) : reader_(reader)
{
}

bool word_counter::done() const
{
        return done_;
}

std::map<std::string, int> word_counter::word_count()
{
        std::string filename = reader_->next_filename();
        if (filename == "")
        {
                done_ = true;
                return {};
        }
        std::ifstream in(filename);
        std::copy(std::istream_iterator<std::string>(in),
                  std::istream_iterator<std::string>(),
                  std::back_inserter(words_));

        std::map<std::string, int> counts;
        for (auto const& word: words_) {
                auto it = counts.find(word);
                if (it == counts.end())
                        counts[word] = 1;
                else
                        it->second++;
        }
        return counts;
}

int main()
{
        bool done = false;
        while (!done)
        {
            auto reader = std::make_shared<input_reader>();
            auto counter = std::make_shared<word_counter>(reader);
            reader->set_counter(counter);
            auto counts = counter->word_count();
            done = counter->done();
            for (auto const& wc : counts)
            {
                std::cout << wc.first << " " << wc.second << '\n';
            }
        }
        return 0;
}
