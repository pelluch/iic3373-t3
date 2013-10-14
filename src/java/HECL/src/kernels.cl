	kernel void equalize_image(read_only image2d_t input, write_only image2d_t output, global float* cdf){
    	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
    	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
    	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE|CLK_ADDRESS_CLAMP|CLK_FILTER_NEAREST; 

    	int x = get_global_id(0);
		int y = get_global_id(1);
		
        int2 coord = (int2)(x,y); 
        float4 pixel_value = read_imagef(input, sampler, coord);
        
        float4 eq_pixel_value = cdf[(int)pixel_value.x];
 		
        write_imagef(output, coord, eq_pixel_value); 
		
	}

	kernel void merge_colHist(global int* colHistograms, global int* histogram, int colCount, int histSize){
		// There's one thread per histogram entry:
		int x = get_global_id(0);
		
		for(int i = 0; i < colCount; i++)
			histogram[x] += colHistograms[histSize * i + x];
	}
	
    kernel void calc_colHist(read_only image2d_t input, int imgHeight, global int* histogram, int histSize) {
    	
    	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
    	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
    	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE|CLK_ADDRESS_CLAMP|CLK_FILTER_NEAREST; 
    
    	int x = get_global_id(0); 
    	
    	for(int y = 0; y < imgHeight; y++)
    	{
			int2 coord = (int2)(x,y); 
			float4 pixel_value = read_imagef(input, sampler, coord);
			
			int bin = round(pixel_value.x);
			
			if(bin < histSize)
				histogram[x*histSize + bin]++;
    	}
    } 
    
    kernel void copy_image(read_only image2d_t input, write_only image2d_t output, const int width, const int height) {
    	
    	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
    	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
    	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE|CLK_ADDRESS_CLAMP|CLK_FILTER_NEAREST; 
    
    	int x = get_global_id(0); 
        int y = get_global_id(1); 
        
        int2 coord = (int2)(x,y); 
        float4 pixel_value = read_imagef(input, sampler, coord);	
 		write_imagef(output, coord, pixel_value); 
    } 

    kernel void convert_to_spherical(read_only image2d_t input, write_only image2d_t output, const int width, const int height) {
        
        const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE|CLK_ADDRESS_CLAMP|CLK_FILTER_NEAREST; 
        int x = get_global_id(0); 
        int y = get_global_id(1); 
        
        int2 coord = (int2)(x,y); 
        float4 pixel_value = read_imagef(input, sampler, coord);    

     }