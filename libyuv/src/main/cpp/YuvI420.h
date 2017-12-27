//
// Created by myb on 12/26/17.
//

#ifndef LIVEVIDEO_YUVI420_H
#define LIVEVIDEO_YUVI420_H

#include <cstdint>

class YuvI420 {
public:
    uint8_t* y;
    uint8_t* u;
    uint8_t* v;
    size_t yStride;
    size_t uStride;
    size_t vStride;
    size_t width;
    size_t height;

    YuvI420(uint8_t *data, int width, int height) {
      yStride = width;
      uStride = vStride = width >> 1;
      y = data;
      u = y + width * height;
      v = u + (width * height >> 2);
      this->width = width;
      this->height = height;
    }
    int length() {
      return width * height * 3/2;
    }
};
#endif //LIVEVIDEO_YUVI420_H
