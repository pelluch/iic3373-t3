    // OpenCL Kernel Function for element by element vector addition
    kernel void calc_hist(global const float** img, global float* hist, int numElements) {

        // get index into global data array
        int iGID = get_global_id(0);

        // bound check, equivalent to the limit on a 'for' loop
        if (iGID >= numElements)  {
            return;
        }

        // add the vector elements
        c[iGID] = a[iGID] + b[iGID];
    }