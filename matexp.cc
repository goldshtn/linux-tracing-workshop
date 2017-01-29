#include <cmath>
#include <iostream>
#include <fstream>
#include <vector>
#include <exception>

template <typename T>
class matrix {
private:
    std::vector<T> data_;
    int rows_, cols_;
public:
    matrix(int rows, int cols) : rows_(rows), cols_(cols) {
        data_.resize(rows * cols);
    }
    T &operator()(int i, int j) {
        return data_[i*cols_ + j];
    }
    T const &operator()(int i, int j) const {
        return data_[i*cols_ + j];
    }
    int height() const { return rows_; }
    int width() const { return cols_; }
    matrix<T> operator+(matrix<T> const &other) const {
        if (height() != other.height() || width() != other.width())
            throw std::invalid_argument("matrix dimensions don't match");
        auto result = *this;
        for (int i = 0; i < rows_; ++i)
            for (int j = 0; j < cols_; ++j)
                result(i, j) += other(i, j);
        return result;
    }
    matrix<T> operator*(matrix<T> const &other) const {
        if (height() != width())
            throw std::invalid_argument("multiplication implemented only "
                                        "for square matrices");
        if (height() != other.height() || width() != other.width())
            throw std::invalid_argument("matrix dimensions don't match");
        matrix<T> result(rows_, cols_);
        for (int i = 0; i < rows_; ++i)
            for (int j = 0; j < cols_; ++j)
                for (int k = 0; k < cols_; ++k)
                    result(i, j) += (*this)(i, k) * other(k, j);
        return result;
    }
};

template <typename T>
class exponentiator {
private:
    matrix<T> mat_;
    int exponent_;
public:
    exponentiator(matrix<T> mat, int exp) :
        mat_(std::move(mat)), exponent_(exp) {
    }
    matrix<T> operator()() {
        auto a = mat_;
        for (int i = 1; i < exponent_; ++i) {
            a = a * mat_;
        }
        return a;
    }
};

class io {
    std::string infile_;

    matrix<float> read() {
        std::ifstream in(infile_.c_str());
        if (!in)
            throw std::invalid_argument("unable to open input file");
        int rows, cols;
        in >> rows;
        in >> cols;
        
        matrix<float> res{ rows, cols };
        for (int i = 0; in; ++i) {
            in >> res(i/cols, i%cols);
        }
        return res;
    }
    void write(matrix<float> const &mat, std::ofstream &out) {
        out << mat.height() << " " << mat.width();
        for (int i = 0; i < mat.height(); ++i) {
            for (int j = 0; j < mat.width(); ++j)
                out << mat(i, j) << " ";
            out << '\n';
        }
    }
public:
    io(std::string filename) : infile_(filename.c_str()) {
    }
    void exp_to(std::string filename, int rounds) {
        std::ofstream out(filename.c_str());
        auto mat = read();
        exponentiator<float> exp(mat, rounds);
        write(exp(), out);
    }
};

int usage() {
    std::cerr << "usage: matexp <infile> <rounds> <outfile>\n";
    return 1;
}

int main(int argc, char *argv[]) {
    if (argc != 4)
        return usage();

    io prog(argv[1]);
    prog.exp_to(argv[3], std::atoi(argv[2]));

    return 0;
}
