//
//  WBNode.m
//  Ring
//
//  Created by chris on 7/5/18.
//  Copyright © 2018 Warebots, LLC. All rights reserved.
//

#import "WBNode.h"

@implementation WBNode

- (instancetype) init {
  self = [super init];
  if(self){
    self.appGroupName = @"";
  }
  return self;
}

- (instancetype) initWithAppGroupName:(NSString *) appGroupName andQueue:(dispatch_queue_t)queue {
  self = [self init];
  if(self){
    self.queue = queue;
    self.appGroupName = [@"group." stringByAppendingString:appGroupName];
    self.containerUrl = [[NSFileManager defaultManager] containerURLForSecurityApplicationGroupIdentifier:self.appGroupName];
    self.containerPath = [self.containerUrl path];
    self.sharedUrl = [self.containerUrl URLByAppendingPathComponent:@"com-warebots-ring"];
    self.sharedPath = [self.sharedUrl path];
    self.dataUrl = [self.sharedUrl URLByAppendingPathComponent:@"data.json"];
    self.dataPath = [self.dataUrl path];
    self.defaults = [[NSUserDefaults alloc] initWithSuiteName:self.appGroupName];
  }
  return self;
}

- (int) getVersion {
  return (int)[self.defaults integerForKey:@"com-warebots-reactnative-ring-version"];
}

- (void) readData:(void (^)(BOOL success, NSDictionary *output))completionBlock {
  dispatch_async(self.queue, ^{
    NSString *dataString = [[NSString alloc] initWithData:[NSData dataWithContentsOfFile:self.dataPath] encoding:NSUTF8StringEncoding];
    int version = (int)[self.defaults integerForKey:@"com-warebots-reactnative-ring-version"];
    NSDictionary *output = @{@"version": @(version), @"data":dataString};
    completionBlock(true, output);
  });
}

- (void) writeData:(int)version data:(NSString *)dataString withCompletion:(void (^)(BOOL success, NSDictionary *output))completionBlock {
  dispatch_async(self.queue, ^{
    NSData *data = [dataString dataUsingEncoding:NSUTF8StringEncoding];
    [data writeToFile:self.dataPath atomically:YES];
    [self.defaults setInteger:version+1 forKey:@"com-warebots-reactnative-ring-version"];
    NSDictionary *output = @{@"version": @(version + 1), @"data":dataString};
    completionBlock(true, output);
  });
}

- (BOOL) assertFilesExist {
  BOOL isDir;
  BOOL dataDirExists = [[NSFileManager defaultManager] fileExistsAtPath:self.sharedPath isDirectory:&isDir];
  if(dataDirExists != YES) {
    [[NSFileManager defaultManager] createDirectoryAtPath:self.sharedPath withIntermediateDirectories:YES attributes:nil error:nil];
    NSString *intialData = @"{}";
    [self.defaults setInteger:-1 forKey:@"com-warebots-reactnative-ring-version"];
    [[NSFileManager defaultManager] createFileAtPath:self.dataPath contents:[intialData dataUsingEncoding:NSUTF8StringEncoding] attributes:nil];
  }
  return YES;
}

@end
