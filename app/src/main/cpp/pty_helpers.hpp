#pragma once

#include <string>
#include <vector>

namespace forge::pty {

inline bool validDimensions(int rows, int columns) {
    return rows > 0 && rows <= 1000 && columns > 0 && columns <= 1000;
}

inline void buildCStringPointers(std::vector<std::string>& values,
                                 std::vector<char*>& pointers) {
    pointers.clear();
    pointers.reserve(values.size() + 1);
    for (auto& value : values) pointers.push_back(value.data());
    pointers.push_back(nullptr);
}

}  // namespace forge::pty
