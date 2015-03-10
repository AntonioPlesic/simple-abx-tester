typedef struct parser parser;

parser* new_parser(char* path);
void delete_parser(parser* d);
void parser_alignToFirst(parser* d);
int parser_getNextFrame(parser* d, char* buffer);
int parser_getCurrentSampleRate(parser* d);
